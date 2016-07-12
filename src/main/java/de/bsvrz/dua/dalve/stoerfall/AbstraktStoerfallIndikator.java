/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
 * Copyright 2015 by Kappich Systemberatung Aachen
 * Copyright 2016 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dua.dalve.
 * 
 * de.bsvrz.dua.dalve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dua.dalve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dua.dalve.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */
package de.bsvrz.dua.dalve.stoerfall;

import de.bsvrz.dav.daf.main.*;
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
 */
public abstract class AbstraktStoerfallIndikator implements
		ClientReceiverInterface, ClientSenderInterface {

	/**
	 * Verbindung zum Datenverteiler
	 */
	protected ClientDavInterface DAV = null;

	/**
	 * Das Objekt, fuer dass der Stoerfallzustand berechnet werden soll
	 */
	protected SystemObject objekt = null;

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
	 * Das zuletzt versendete Ergebnis
	 */
	private ResultData _letztesErgebnis;

	/**
	 * Initialisiert diese Instanz indem sich auf Parameter angemeldet wird und eine Sendeanmeldung durchgefuehrt wird.
	 *
	 * @param dav    Datenverteiler-Verbindung
	 * @param objekt das Objekt, fuer dass der Stoerfallzustand berechnet werden soll
	 * @throws DUAInitialisierungsException wenn dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public void initialisiere(final ClientDavInterface dav,
			final SystemObject objekt) throws DUAInitialisierungsException {
		if(DAV == null) {
			DAV = dav;
		}
		this.objekt = objekt;

		if(this.getParameterAtgPid() != null) {
			this.paraAtg = dav.getDataModel().getAttributeGroup(
					this.getParameterAtgPid());
			dav.subscribeReceiver(this, objekt, new DataDescription(
					                      this.paraAtg, dav.getDataModel().getAspect(
					DaVKonstanten.ASP_PARAMETER_SOLL)),
			                      ReceiveOptions.normal(), ReceiverRole.receiver()
			);
		}

		this.pubBeschreibung = new DataDescription(dav.getDataModel()
				                                           .getAttributeGroup(DUAKonstanten.ATG_STOERFALL_ZUSTAND), dav
				                                           .getDataModel().getAspect(this.getPubAspektPid()));
		try {
			dav.subscribeSender(this, objekt, this.pubBeschreibung, SenderRole
					.source());
		}
		catch(OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException("", e);
		}
	}

	/**
	 * Macht alle Anmeldungen aus dem Konstruktor wieder rueckgaengig.
	 */
	protected void abmelden() throws DUAInitialisierungsException {
		if(this.paraAtg != null) {
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
	 * @param parameter einen Parametersatz
	 */
	protected void readParameter(ResultData parameter) {
		// zum ueberschreiben bzw. weglassen gedacht
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten und publiziert diesen ggf.
	 *
	 * @param resultat ein empfangenes Datum zur Berechnung des Stoerfallindikators
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
		if(resultate != null) {
			for(ResultData resultat : resultate) {
				if(resultat != null) {
					if(this.paraAtg != null
							&& resultat.getDataDescription()
							.getAttributeGroup().getId() == this.paraAtg
							.getId()) {
						/**
						 * Parameter empfangen
						 */
						this.readParameter(resultat);
					}
					else {
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
	protected final void sendeErgebnis(final ResultData ergebnis) {
		if(ergebnis.getData() != null) {
			try {
				DAV.sendData(ergebnis);
				this.aktuellKeineDaten = false;
			}
			catch(DataNotSubscribedException | SendSubscriptionNotConfirmed e) {
				Debug.getLogger().error("", e);
				e.printStackTrace();
			}
		}
		else {
			if(!this.aktuellKeineDaten) {
				try {
					DAV.sendData(ergebnis);
					this.aktuellKeineDaten = true;
				}
				catch(DataNotSubscribedException | SendSubscriptionNotConfirmed e) {
					Debug.getLogger().error("", e);
					e.printStackTrace();
				}
			}
		}
		_letztesErgebnis = ergebnis;
	}

	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object,
			DataDescription dataDescription, byte state) {
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object,
			DataDescription dataDescription) {
		return false;
	}

}
