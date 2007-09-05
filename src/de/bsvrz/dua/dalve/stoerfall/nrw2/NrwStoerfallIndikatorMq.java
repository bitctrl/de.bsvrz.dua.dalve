/**
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:<br>
 * BitCtrl Systems GmbH<br>
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve.stoerfall.nrw2;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.AttributeGroup;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.dua.dalve.prognose.PrognoseSystemObjekt;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.FahrStreifen;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.StoerfallSituation;

/**
 * Repräsentiert einen Stoerfallindikator nach Verfahren NRW (nur fuer Messquerschnitte)
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class NrwStoerfallIndikatorMq 
extends NrwStoerfallIndikatorFs{

	/**
	 * Mappt alle Fahrstreifen dieses Messquerschnitts auf deren letztes
	 * empfangenes Analysedatum
	 */
	private Map<SystemObject, ResultData> fsDaten = new HashMap<SystemObject, ResultData>();
	
	/**
	 * Attributgruppe der Analysedaten von Fahrstreifen
	 */
	private AttributeGroup fsAnalyseAtg = null; 
	
	
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(ClientDavInterface dav,
							  PrognoseSystemObjekt objekt)
	throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);
		
		/**
		 * Anmeldung auf Analysedaten aller Fahrstreifen dieses 
		 * Messquerschnittes
		 */
		MessQuerschnitt mq = MessQuerschnitt.getInstanz(objekt.getObjekt());
		if(mq != null){
			for(FahrStreifen fs:mq.getFahrStreifen()){
				this.fsDaten.put(fs.getSystemObject(), null);
			}
			
			this.fsAnalyseAtg = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS);
			
			dav.subscribeReceiver(this,
								  fsDaten.keySet(),
								  new DataDescription(
								  this.fsAnalyseAtg,
								  dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE),
								  (short)0),
								  ReceiveOptions.normal(),
								  ReceiverRole.receiver());			
		}else{
			throw new DUAInitialisierungsException("Messquerschnitt " + objekt + //$NON-NLS-1$
					" konnte nicht vollstaendig ausgelesen werden"); //$NON-NLS-1$
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParameterAtgPid() {
		return "atg.verkehrsLageVerfahren2"; //$NON-NLS-1$
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenNRW"; //$NON-NLS-1$
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void berechneStoerfallIndikator(ResultData resultat) {
		Data data = null;

		if(resultat.getData() != null){	
			if(resultat.getDataDescription().getAttributeGroup().equals(objekt.getAnalyseAtg())){
				this.analyseDatensatz = resultat;
			}else
			if(resultat.getDataDescription().getAttributeGroup().equals(this.fsAnalyseAtg)){
				this.fsDaten.put(resultat.getObject(), resultat);
			}else{
				this.geglaettetDatensatz = resultat;
			}

			/**
			 * Führe jetzt eine Berechnung durch, wenn alle Daten von allen Fahrstreifen 
			 * fuer ein und denselben Zeitstempel da
			 */
			TreeSet<Long> zeitStempelPuffer = new TreeSet<Long>();
			for(SystemObject fsObj:this.fsDaten.keySet()){
				ResultData fsDatum = this.fsDaten.get(fsObj);
				if(fsDatum == null){
					zeitStempelPuffer = null;
					break;
				}else{
					zeitStempelPuffer.add(fsDatum.getDataTime());
				}
			}

			if(zeitStempelPuffer != null &&
					zeitStempelPuffer.size() == 1 && 
					this.analyseDatensatz != null &&
					this.geglaettetDatensatz != null &&
					this.analyseDatensatz.getDataTime() == this.geglaettetDatensatz.getDataTime() &&
					this.analyseDatensatz.getDataTime() == zeitStempelPuffer.first()){										
				StoerfallSituation stufe = StoerfallSituation.KEINE_AUSSAGE;

				if(v1 >= 0 && v2 >= 0 &&
				  k1 >= 0 && k2 >= 0 && k3 >= 0 && kT >= 0){

					long QKfzGNormal = this.geglaettetDatensatz.getData().getItem("QKfzG").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
					long VKfz = this.analyseDatensatz.getData().getItem("VKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$

					if(QKfzGNormal >= 0 && VKfz > 0){
						double kvst = (double)QKfzGNormal / (double)VKfz;
						
						/**
						 * Ermittlung von vvst
						 */						
						double vvst = this.getVVST(VKfz);

						stufe = this.getVerkehrsStufe(kvst, vvst);
						this.letzteStufe = stufe;
						
						StoerfallZustand zustand = new StoerfallZustand(DAV);
						zustand.setSituation(stufe);
						data = zustand.getData();
					}		

				}
			}else{
				ResultData ergebnis = new ResultData(this.objekt.getObjekt(), 
						this.pubBeschreibung, resultat.getDataTime(), data);
				this.sendeErgebnis(ergebnis);					
			}				

		}else{
			ResultData ergebnis = new ResultData(this.objekt.getObjekt(), 
					this.pubBeschreibung, resultat.getDataTime(), data);
			this.sendeErgebnis(ergebnis);
		}
	}
	
	
	/**
	 * Ermittelt den Parameter VVST zur Berechnung der Verkehrslage analog zu
	 * den Vorgaben aus SE-02.00.00.00.00-AFo-4.0 (S.155)<br><br>
	 * <code>VVST = MIN(vgKfz(fs1), vgKfz(fs2), ... , vgKfz(fsN))</code>,<br> wenn
	 * die geglaettete mittlere Geschwindigkeit (TLS) vorliegt und <code>VKfz</code>
	 * sonst <br><br>
	 * <b>Achtung:</b> Diese Methode darf nur aufgerufen werden, wenn fuer alle
	 * in diesem Messquerschnitt betrachteten Fahstreifen Daten mit dem gleichen 
	 * Zeitstempel verfuegbar sind
	 * 
	 * @param VKfz wird benutzt, wenn die geglaettete mittlere Geschwindigkeit 
	 * (TLS) nicht vorliegt
	 * @return der Parameter VVST zur Berechnung der Verkehrslage
	 */
	private final long getVVST(final long VKfz){
		long vvst = VKfz;
		long min = Long.MAX_VALUE;
		
		for(ResultData fsDatum:this.fsDaten.values()){
			long vgKfz = fsDatum.getData().getItem("vgKfz"). //$NON-NLS-1$
						getUnscaledValue("Wert").longValue(); //$NON-NLS-1$
			if(vgKfz < min){
				min = vgKfz;
			}
		}
		if(min >= 0){
			vvst = min;
		}
		
		return vvst;
	}
	
}
