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

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.DataNotSubscribedException;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SendSubscriptionNotConfirmed;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Repräsentiert einen Stoerfallindikator
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 */
public abstract class AbstraktStoerfallIndikator implements
		ClientReceiverInterface, ClientSenderInterface {

	/**
	 * Verbindung zum Datenverteiler
	 */
	protected static ClientDavInterface DAV = null;

	/**
	 * Das Objekt, fuer dass der Stoerfallzustand berechnet werden soll
	 */
	protected SystemObject objekt = null;

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
	 * Indiziert, ob dieses Objekt im Moment auf <code>keine Daten</code>
	 * steht
	 */
	protected boolean aktuellKeineDaten = true;

	/**
	 * Initialisiert diese Instanz indem sich auf Parameter angemeldet wird und
	 * eine Sendeanmeldung durchgefuehrt wird.
	 * 
	 * @param dav
	 *            Datenverteiler-Verbindung
	 * @param objekt
	 *            das Objekt, fuer dass der Stoerfallzustand berechnet werden
	 *            soll
	 * @throws DUAInitialisierungsException
	 *             wenn dieses Objekt nicht vollständig initialisiert werden
	 *             konnte
	 */
	public void initialisiere(final ClientDavInterface dav,
			final SystemObject objekt) throws DUAInitialisierungsException {
		if (DAV == null) {
			DAV = dav;
		}
		this.objekt = objekt;

		if (this.getParameterAtgPid() != null) {
			this.paraAtg = dav.getDataModel().getAttributeGroup(
					this.getParameterAtgPid());
			dav.subscribeReceiver(this, objekt, new DataDescription(
					this.paraAtg, dav.getDataModel().getAspect(
							DaVKonstanten.ASP_PARAMETER_SOLL)),
					ReceiveOptions.normal(), ReceiverRole.receiver());
		}

		this.pubBeschreibung = new DataDescription(dav.getDataModel()
				.getAttributeGroup(DUAKonstanten.ATG_STOERFALL_ZUSTAND), dav
				.getDataModel().getAspect(this.getPubAspektPid()));
		try {
			dav.subscribeSender(this, objekt, this.pubBeschreibung, SenderRole
					.source());
		} catch (OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException(Constants.EMPTY_STRING, e);
		}
	}
	
	/**
	 * Macht alle Anmeldungen aus dem Konstruktor wieder rueckgaengig. 
	 */
	protected void abmelden() throws DUAInitialisierungsException {
		if (this.paraAtg != null) {
			DAV.unsubscribeReceiver(this, objekt, new DataDescription(
					this.paraAtg, DAV.getDataModel().getAspect(
							DaVKonstanten.ASP_PARAMETER_SOLL)));
		}
		DAV.unsubscribeSender(this, objekt, this.pubBeschreibung);
		this.paraAtg = null;
		this.objekt = null;
		this.pubBeschreibung = null;
	}

	/**
	 * Erfragt die Pid der Parameterattributgruppe
	 * 
	 * @return die Pid der Parameterattributgruppe
	 */
	protected String getParameterAtgPid() {
		return null;
	}

	/**
	 * Liest einen Parametersatz
	 * 
	 * @param parameter
	 *            einen Parametersatz
	 */
	protected void readParameter(ResultData parameter) {
		// zum ueberschreiben bzw. weglassen gedacht
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten
	 * und publiziert diesen ggf.
	 * 
	 * @param resultat
	 *            ein empfangenes Datum zur Berechnung des Stoerfallindikators
	 */
	protected abstract void berechneStoerfallIndikator(ResultData resultat);

	/**
	 * Erfragt die Pid des Publikationsaspektes
	 * 
	 * @return die Pid der Publikationsaspektes
	 */
	protected abstract String getPubAspektPid();

	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if (resultate != null) {
			for (ResultData resultat : resultate) {
				if (resultat != null) {
					if (this.paraAtg != null
							&& resultat.getDataDescription()
									.getAttributeGroup().getId() == this.paraAtg
									.getId()) {
						/**
						 * Parameter empfangen
						 */
						this.readParameter(resultat);
					} else {
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
	 * @param ergebnis
	 *            ein Ergebnisdatensatz
	 */
	protected final void sendeErgebnis(final ResultData ergebnis) {
		if (ergebnis.getData() != null) {
			try {
				if (sendenOk) {
					DAV.sendData(ergebnis);
					this.aktuellKeineDaten = false;
				} else {
					Debug.getLogger().info(
							"Keine Abnehmer fuer Daten von " + this.objekt); //$NON-NLS-1$
				}
			} catch (DataNotSubscribedException e) {
				Debug.getLogger().error(Constants.EMPTY_STRING, e);
				e.printStackTrace();
			} catch (SendSubscriptionNotConfirmed e) {
				Debug.getLogger().error(Constants.EMPTY_STRING, e);
				e.printStackTrace();
			}
		} else {
			if (!this.aktuellKeineDaten) {
				try {
					if (sendenOk) {
						DAV.sendData(ergebnis);
						this.aktuellKeineDaten = true;
					} else {
						Debug.getLogger().info(
								"Keine Abnehmer fuer Daten von " + this.objekt); //$NON-NLS-1$
					}
				} catch (DataNotSubscribedException e) {
					Debug.getLogger().error(Constants.EMPTY_STRING, e);
					e.printStackTrace();
				} catch (SendSubscriptionNotConfirmed e) {
					Debug.getLogger().error(Constants.EMPTY_STRING, e);
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
