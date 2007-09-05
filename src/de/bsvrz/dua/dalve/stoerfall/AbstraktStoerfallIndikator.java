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
package de.bsvrz.dua.dalve.stoerfall;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.ClientSenderInterface;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.common.OneSubscriptionPerSendData;
import stauma.dav.configuration.interfaces.AttributeGroup;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.debug.Debug;
import de.bsvrz.dua.dalve.prognose.PrognoseSystemObjekt;
import de.bsvrz.dua.dalve.prognose.PrognoseTyp;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Repräsentiert einen Stoerfallindikator
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public abstract class AbstraktStoerfallIndikator
implements ClientReceiverInterface, ClientSenderInterface{
	
	/**
	 * Debug-Logger
	 */
	protected static final Debug LOGGER = Debug.getLogger();
		
	/**
	 * Verbindung zum Datenverteiler
	 */
	protected static ClientDavInterface DAV = null;
	
	/**
	 * Das Objekt, fuer dass der Stoerfallzustand berechnet werden soll
	 */
	protected PrognoseSystemObjekt objekt = null;
	
	/**
	 * Indiziert, ob ein Abnehmer für die Daten dieses Objektes da ist
	 */
	protected boolean sendenOk = false;
		
	/**
	 * Parameter Attributgruppe
	 */
	protected AttributeGroup paraAtg = null;

	/**
	 * Datenbeschreibung der zu publizierenden Daten
	 */
	protected DataDescription pubBeschreibung = null;
	
	/**
	 * Indiziert, ob dieses Objekt im Moment auf <code>keine Daten</code> steht
	 */
	protected boolean aktuellKeineDaten = true;
	
	/**
	 * letzter versendeter Störfallzustand
	 */
	protected ResultData letztesErgebnis = null; 


		
	/**
	 * Initialisiert diese Instanz
	 * 
	 * @param dav Datenverteiler-Verbindung
	 * @param objekt das Objekt, fuer dass der Stoerfallzustand berechnet werden soll
	 * @throws DUAInitialisierungsException wenn dieses Objekt nicht vollständig
	 * initialisiert werden konnte
	 */
	public void initialisiere(final ClientDavInterface dav,
							  final PrognoseSystemObjekt objekt)
	throws DUAInitialisierungsException{
		if(DAV == null){
			DAV = dav;
		}
		this.objekt = objekt;
		
		this.paraAtg = dav.getDataModel().getAttributeGroup(this.getParameterAtgPid());
		dav.subscribeReceiver(this, objekt.getObjekt(),
				new DataDescription(this.paraAtg, dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL), (short)0),
				ReceiveOptions.normal(), ReceiverRole.receiver());
		
		
		this.pubBeschreibung = new DataDescription(
				dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_STOERFALL_ZUSTAND),
				dav.getDataModel().getAspect(this.getPubAspektPid()),
				(short)0);
		try {
			dav.subscribeSender(this, objekt.getObjekt(), this.pubBeschreibung, SenderRole.source());
		} catch (OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException(Konstante.LEERSTRING, e);
		}
		
		/**
		 * Anmeldung auf Daten
		 */
		dav.subscribeReceiver(this, objekt.getObjekt(),
				new DataDescription(this.objekt.getPubAtgGlatt(),
						PrognoseTyp.NORMAL.getAspekt(),
						(short)0),
						ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	
	/**
	 * Erfragt die Pid der Parameterattributgruppe
	 * 
	 * @return die Pid der Parameterattributgruppe
	 */
	protected abstract String getParameterAtgPid();

	
	/**
	 * Erfragt die Pid des Publikationsaspektes
	 * 
	 * @return die Pid der Publikationsaspektes
	 */
	protected abstract String getPubAspektPid();


	/**
	 * Liest einen Parametersatz
	 * 
	 * @param parameter einen Parametersatz
	 */
	protected abstract void readParameter(ResultData parameter);
	
	
	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten
	 * und publiziert diesen ggf.
	 * 
	 * @param resultat ein empfangenes Datum zur Berechnung des Stoerfallindikators
	 */
	protected abstract void berechneStoerfallIndikator(ResultData resultat);
	
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null) {
			for(ResultData resultat:resultate){
				if(resultat != null){
					if(resultat.getDataDescription().getAttributeGroup().getId() == this.paraAtg.getId()){
						/**
						 * Parameter empfangen
						 */
						this.readParameter(resultat);
					}else{
						/**
						 * Daten empfangen
						 */
						this.berechneStoerfallIndikator(resultat);							
					}
				}
			}
		}
	}
	
	
	/**
	 * Sendet einen Ergebnisdatensatz
	 * 
	 * @param ergebnis ein Ergebnisdatensatz
	 */
	protected final void sendeErgebnis(final ResultData ergebnis){
		if(ergebnis.getData() != null){
			try {
				if(true/*sendenOk*/){
					letztesErgebnis = ergebnis;
					DAV.sendData(letztesErgebnis);
					this.letztesErgebnis = null;
					this.aktuellKeineDaten = false;
				}else{
					LOGGER.info("Keine Abnehmer fuer Daten von " + this.objekt); //$NON-NLS-1$
				}
			} catch (Exception e) {
				LOGGER.error(Konstante.LEERSTRING, e);
				e.printStackTrace();
			}
		}else{
			if(!this.aktuellKeineDaten){
				try {
					if(sendenOk){
						letztesErgebnis = ergebnis;
						DAV.sendData(letztesErgebnis);
						this.letztesErgebnis = null;
						this.aktuellKeineDaten = true;
					}else{
						LOGGER.info("Keine Abnehmer fuer Daten von " + this.objekt); //$NON-NLS-1$
					}
				} catch (Exception e) {
					LOGGER.error(Konstante.LEERSTRING, e);
					e.printStackTrace();
				}
			}							
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object,
			DataDescription dataDescription, byte state) {
		if(!this.sendenOk && 
			state == ClientSenderInterface.START_SENDING && 
			this.letztesErgebnis != null){
			try {
				DAV.sendData(letztesErgebnis);
				this.letztesErgebnis = null;
			} catch (Exception e) {
				LOGGER.error(Konstante.LEERSTRING, e);
				e.printStackTrace();
			}
		}
		this.sendenOk = state == ClientSenderInterface.START_SENDING;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object,
			DataDescription dataDescription) {
		return true;
	}
	
}
