package de.bsvrz.dua.dalve;

import java.util.HashMap;
import java.util.Map;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.ClientSenderInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.common.OneSubscriptionPerSendData;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

public abstract class AbstraktPrognoseObjekt 
implements ClientReceiverInterface,
		   ClientSenderInterface{

	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	private static ClientDavInterface DAV = null;
	
	private AtgPrognoseParameter parameter = null; 
	
	private PrognoseSystemObjekt prognoseObjekt = null;
	

	private DataDescription pubBeschreibungGlatt = null;
		
	private DataDescription pubBeschreibungPrognose = null;
	
	/**
	 * zeigt an, ob dieses Objekt im Moment auf keine Daten steht
	 */
	private boolean aktuellKeineDaten = true;
	
	
	private Map<PrognoseAttribut, DavAttributPrognoseObjekt> attributePuffer = 
						new HashMap<PrognoseAttribut, DavAttributPrognoseObjekt>();
	
		
	/**
	 * Standardkonstruktor 
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param prognoseObjekt das Prognoseobjekt, für das prognostiziert werden soll
	 */
	public AbstraktPrognoseObjekt(final ClientDavInterface dav, 
								  final PrognoseSystemObjekt prognoseObjekt)
	throws DUAInitialisierungsException{
		if(DAV == null){
			DAV = dav;			
		}
		this.prognoseObjekt = prognoseObjekt;
				
		/**
		 * Auf Parameter anmelden
		 */
		this.parameter = new AtgPrognoseParameter(DAV, prognoseObjekt, this.getPrognoseTyp());

		/**
		 * Alle Prognoseattribute initialisieren
		 */
		for(PrognoseAttribut attribut:PrognoseAttribut.getInstanzen()){
			DavAttributPrognoseObjekt attributPrognose = new DavAttributPrognoseObjekt(prognoseObjekt);
			this.parameter.addListener(attributPrognose, attribut);
			this.attributePuffer.put(attribut, attributPrognose);
		}
		
		/**
		 * Sendeanmeldungen fuer Prognosewerte und geglaetttete Werte
		 */
		this.pubBeschreibungPrognose = new DataDescription(prognoseObjekt.getPubAtgPrognose(), 
														   this.getPrognoseTyp().getAspekt(), 
														   (short)0);
		this.pubBeschreibungGlatt = new DataDescription(prognoseObjekt.getPubAtgGlatt(), 
														this.getPrognoseTyp().getAspekt(), 
														(short)0);
		try {
			DAV.subscribeSender(this, prognoseObjekt.getObjekt(), this.pubBeschreibungPrognose, SenderRole.source());
			DAV.subscribeSender(this, prognoseObjekt.getObjekt(), this.pubBeschreibungGlatt, SenderRole.source());
		} catch (OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException(Konstante.LEERSTRING, e);
		}
		
		/**
		 * Impliziter Start des Objektes: Anmeldung auf Daten
		 */
		DAV.subscribeReceiver(this, prognoseObjekt.getObjekt(), 
				new DataDescription(prognoseObjekt.getQuellAtg(), 
									DAV.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
									(short)0),
									ReceiveOptions.normal(), ReceiverRole.receiver());
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					boolean datenSenden = true;
					Data glaettungNutzdaten = null;
					Data prognoseNutzdaten = null;

					try{
						for(PrognoseAttribut attribut:PrognoseAttribut.getInstanzen()){
							this.attributePuffer.get(attribut).aktualisiere(resultat);
						}
						
						if(resultat.getData() != null){
							aktuellKeineDaten = false;
							long T = resultat.getData().getTimeValue("T").getMillis(); //$NON-NLS-1$
							
							/**
							 * Baue geglaetteten Ergebniswert zusammen:
							 */
							glaettungNutzdaten = DAV.createData(this.prognoseObjekt.getPubAtgGlatt());
							glaettungNutzdaten.getTimeValue("T").setMillis(T); //$NON-NLS-1$
							for(PrognoseAttribut attribut:PrognoseAttribut.getInstanzen()){
								this.attributePuffer.get(attribut).exportiereDatenGlatt(glaettungNutzdaten);
							}
							
							/**
							 * Baue Prognosewert zusammen:
							 */
							prognoseNutzdaten = DAV.createData(this.prognoseObjekt.getPubAtgPrognose());
							prognoseNutzdaten.getTimeValue("T").setMillis(T); //$NON-NLS-1$
							for(PrognoseAttribut attribut:PrognoseAttribut.getInstanzen()){
								this.attributePuffer.get(attribut).exportiereDatenPrognose(prognoseNutzdaten);
							}
						}else{						
							if(aktuellKeineDaten){
								datenSenden = false;
							}
							aktuellKeineDaten = true;
						}
					}catch(PrognoseParameterException e){
						LOGGER.error("Prognosedaten koennen fuer " + this.prognoseObjekt.getObjekt() //$NON-NLS-1$ 
								+ " nicht berechnet werden", e); //$NON-NLS-1$
						e.printStackTrace();
						
						if(aktuellKeineDaten){
							datenSenden = false;
						}						
						aktuellKeineDaten = true;
					}

					if(datenSenden){
						ResultData glaettungsDatum = new ResultData(this.prognoseObjekt.getObjekt(),
																	this.pubBeschreibungGlatt, 
																	resultat.getDataTime(),
																	glaettungNutzdaten);
						ResultData prognoseDatum = new ResultData(this.prognoseObjekt.getObjekt(),
																	this.pubBeschreibungPrognose, 
																	resultat.getDataTime(),
																	prognoseNutzdaten);

						try {
							DAV.sendData(glaettungsDatum);
							DAV.sendData(prognoseDatum);
						} catch (Exception e) {
							LOGGER.error("Prognosedaten konnten nicht gesendet werden", e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object,
							DataDescription dataDescription, byte state) {
		// 
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object,
									  DataDescription dataDescription) {
		return false;
	}
	
	
	/**
	 * Abstrakte Methoden
	 */
	
	/**
	 * 
	 * @return
	 */
	protected abstract PrognoseTyp getPrognoseTyp();
	
}
