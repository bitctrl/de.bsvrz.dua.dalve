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
package de.bsvrz.dua.dalve.prognose;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.HashSet;
import java.util.Set;

/**
 * Ueber dieses Objekt werden die Prognosedaten fuer <b>einen</b> Fahrstreifen
 * oder einen Messquerschnitt erstellt/publiziert bzw. deren Erstellung
 * verwaltet
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 */
public abstract class AbstraktPrognoseObjekt implements
		ClientReceiverInterface, ClientSenderInterface {

	/**
	 * Verbindung zum Datenverteiler
	 */
	private ClientDavInterface DAV = null;

	/**
	 * Die Parameter, die die Erstellung der Daten steuern (alpha, beta, etc.)
	 */
	private AtgPrognoseParameter parameter = null;

	/**
	 * Das Objekt, fuer das Prognosedaten und geglaettete Daten erstellt werden
	 * sollen (Fahrstreifen oder Messquerschnitt)
	 */
	private SystemObject prognoseObjekt = null;

	/**
	 * Publikationsbeschreibung der geglaetteten Daten
	 */
	private DataDescription pubBeschreibungGlatt = null;

	/**
	 * Publikationsbeschreibung der Prognosedaten
	 */
	private DataDescription pubBeschreibungPrognose = null;

	/**
	 * zeigt an, ob dieses Objekt im Moment auf keine Daten steht
	 */
	private boolean aktuellKeineDaten = true;

	/**
	 * Fuer jedes zu berechnende Attribut (des Zieldatums) ein Prognoseobjekt
	 */
	private Set<DavAttributPrognoseObjekt> attributePuffer = new HashSet<DavAttributPrognoseObjekt>();

	/**
	 * Initialisiert dieses Objekt. Nach Beendigung dieser Methode empfängt und
	 * publiziert dieses Objekt Daten
	 * 
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 * @param prognoseObjekt
	 *            das Prognoseobjekt, für das prognostiziert werden soll
	 * @throws DUAInitialisierungsException
	 *             wenn die Sendeanmeldung fehlschlaegt
	 */
	public final void initialisiere(final ClientDavInterface dav,
			final SystemObject prognoseObjekt)
			throws DUAInitialisierungsException {
		if (DAV == null) {
			DAV = dav;
		}
		this.prognoseObjekt = prognoseObjekt;

		/**
		 * Auf Parameter anmelden
		 */
		this.parameter = new AtgPrognoseParameter(DAV, prognoseObjekt, this
				.getPrognoseTyp());

		/**
		 * Alle Prognoseattribute initialisieren
		 */
		for (PrognoseAttribut attribut : PrognoseAttribut.getInstanzen()) {
			DavAttributPrognoseObjekt attributPrognose = new DavAttributPrognoseObjekt(
					prognoseObjekt, attribut, this.getPrognoseTyp());
			this.parameter.addListener(attributPrognose, attribut);
			this.attributePuffer.add(attributPrognose);
		}

		/**
		 * Sendeanmeldungen fuer Prognosewerte und geglaetttete Werte
		 */
		this.pubBeschreibungPrognose = new DataDescription(DatenaufbereitungLVE
				.getPubAtgPrognose(prognoseObjekt), this.getPrognoseTyp()
				.getAspekt());
		this.pubBeschreibungGlatt = new DataDescription(DatenaufbereitungLVE
				.getPubAtgGlatt(prognoseObjekt), this.getPrognoseTyp()
				.getAspekt());
		try {
			DAV.subscribeSender(this, prognoseObjekt,
					this.pubBeschreibungPrognose, SenderRole.source());
			DAV.subscribeSender(this, prognoseObjekt,
					this.pubBeschreibungGlatt, SenderRole.source());
		} catch (OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException("", e);
		}

		/**
		 * Impliziter Start des Objektes: Anmeldung auf Daten
		 */
		DAV.subscribeReceiver(this, prognoseObjekt, new DataDescription(
				DatenaufbereitungLVE.getAnalyseAtg(prognoseObjekt), DAV
						.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	public void update(ResultData[] resultate) {
		if (resultate != null) {
			for (ResultData resultat : resultate) {
				if (resultat != null) {
					boolean datenSenden = true;
					Data glaettungNutzdaten = null;
					Data prognoseNutzdaten = null;

					try {
						for (DavAttributPrognoseObjekt attribut : this.attributePuffer) {
							attribut.aktualisiere(resultat);
						}

						if (resultat.getData() != null) {
							aktuellKeineDaten = false;

							/**
							 * Baue geglaetteten Ergebniswert zusammen:
							 */
							glaettungNutzdaten = DAV
									.createData(DatenaufbereitungLVE
											.getPubAtgGlatt(prognoseObjekt));
							for (DavAttributPrognoseObjekt attribut : this.attributePuffer) {
								attribut
										.exportiereDatenGlatt(glaettungNutzdaten);
							}
							this.fuegeKBPHinzu(glaettungNutzdaten, false,
									this.attributePuffer);

							/**
							 * Baue Prognosewert zusammen:
							 */
							prognoseNutzdaten = DAV
									.createData(DatenaufbereitungLVE
											.getPubAtgPrognose(this.prognoseObjekt));
							for (DavAttributPrognoseObjekt attribut : this.attributePuffer) {
								attribut
										.exportiereDatenPrognose(prognoseNutzdaten);
							}
							this.fuegeKBPHinzu(prognoseNutzdaten, true,
									this.attributePuffer);

							if (this.prognoseObjekt
									.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
								long T = resultat.getData()
										.getTimeValue("T").getMillis(); //$NON-NLS-1$
								glaettungNutzdaten
										.getTimeValue("T").setMillis(T); //$NON-NLS-1$
								prognoseNutzdaten
										.getTimeValue("T").setMillis(T); //$NON-NLS-1$
							}

						} else {
							if (aktuellKeineDaten) {
								datenSenden = false;
							}
							aktuellKeineDaten = true;
						}
					} catch (PrognoseParameterException e) {
						Debug
								.getLogger()
								.warning(
										"Prognosedaten koennen fuer " + this.prognoseObjekt //$NON-NLS-1$ 
												+ " nicht berechnet werden:\n" + e.getMessage()); //$NON-NLS-1$
						if (aktuellKeineDaten) {
							datenSenden = false;
						}
						aktuellKeineDaten = true;
					}

					if (datenSenden) {
						ResultData glaettungsDatum = new ResultData(
								this.prognoseObjekt, this.pubBeschreibungGlatt,
								resultat.getDataTime(), glaettungNutzdaten);
						ResultData prognoseDatum = new ResultData(
								this.prognoseObjekt,
								this.pubBeschreibungPrognose, resultat
										.getDataTime(), prognoseNutzdaten);

						try {
							DAV.sendData(glaettungsDatum);
						} catch (DataNotSubscribedException e) {
							Debug
									.getLogger()
									.error(
											"Geglaettete Daten konnten nicht gesendet werden: " + glaettungsDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						} catch (SendSubscriptionNotConfirmed e) {
							Debug
									.getLogger()
									.error(
											"Geglaettete Daten konnten nicht gesendet werden: " + glaettungsDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}

						try {
							DAV.sendData(prognoseDatum);
						} catch (DataNotSubscribedException e) {
							Debug
									.getLogger()
									.error(
											"Prognosedaten konnten nicht gesendet werden: " + prognoseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						} catch (SendSubscriptionNotConfirmed e) {
							Debug
									.getLogger()
									.error(
											"Prognosedaten konnten nicht gesendet werden: " + prognoseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * Fuegt einem errechneten Prognosedatum bzw. einem geglaetteten Datum den
	 * Wert <code>k(K)BP</code> bzw. <code>k(K)BG</code> hinzu
	 * 
	 * @param zielDatum
	 *            ein veraenderbares Zieldatum der Attributgruppe
	 * @param prognose
	 *            ob ein Prognosewert veraendert werden soll
	 * @param attributPuffer
	 *            ungerundete Prognosewerte
	 */
	private final void fuegeKBPHinzu(Data zielDatum, final boolean prognose,
			final Set<DavAttributPrognoseObjekt> attributPuffer) {
		DaMesswertUnskaliert qb = null;
		double qBOrig = 0;
		DaMesswertUnskaliert vKfz = null;
		double vKfzOrig = 0;
		MesswertUnskaliert kb = null;
		GWert gueteVKfz = null;
		GWert guetekb = null;

		for (DavAttributPrognoseObjekt attribut : attributPuffer) {
			if (prognose) {
				if (attribut.getAttribut().equals(PrognoseAttribut.QB)) {
					qBOrig = attribut.getZP();
				}
				if (attribut.getAttribut().equals(PrognoseAttribut.V_KFZ)) {
					vKfzOrig = attribut.getZP();
				}
			} else {
				if (attribut.getAttribut().equals(PrognoseAttribut.QB)) {
					qBOrig = attribut.getZP();
				}
				if (attribut.getAttribut().equals(PrognoseAttribut.V_KFZ)) {
					vKfzOrig = attribut.getZG();
				}
			}

		}

		if (prognose) {
			qb = new DaMesswertUnskaliert(PrognoseAttribut.QB
					.getAttributNamePrognose(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)),
					zielDatum);
			vKfz = new DaMesswertUnskaliert(PrognoseAttribut.V_KFZ
					.getAttributNamePrognose(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)),
					zielDatum);
			kb = new MesswertUnskaliert(PrognoseAttribut.KB
					.getAttributNamePrognose(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			guetekb = new GWert(zielDatum, PrognoseAttribut.QB
					.getAttributNamePrognose(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			gueteVKfz = new GWert(zielDatum, PrognoseAttribut.V_KFZ
					.getAttributNamePrognose(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
		} else {
			qb = new DaMesswertUnskaliert(PrognoseAttribut.QB
					.getAttributNameGlatt(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)),
					zielDatum);
			vKfz = new DaMesswertUnskaliert(PrognoseAttribut.V_KFZ
					.getAttributNameGlatt(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)),
					zielDatum);
			kb = new MesswertUnskaliert(PrognoseAttribut.KB
					.getAttributNameGlatt(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			guetekb = new GWert(zielDatum, PrognoseAttribut.QB
					.getAttributNameGlatt(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			gueteVKfz = new GWert(zielDatum, PrognoseAttribut.V_KFZ
					.getAttributNameGlatt(this.prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
		}

		kb.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		if (vKfz.getWertUnskaliert() > 0) {
			if (qb.getWertUnskaliert() >= 0) {
				kb.setWertUnskaliert(Math.round((double) qBOrig
						/ (double) vKfzOrig));
				if (DUAUtensilien.isWertInWerteBereich(zielDatum.getItem(
						kb.getName()).getItem("Wert"), kb.getWertUnskaliert())) { //$NON-NLS-1$
					kb.setWertUnskaliert(kb.getWertUnskaliert());
					kb.setInterpoliert(qb.isPlausibilisiert()
							|| vKfz.isPlausibilisiert());
				}
			}
		}

		GWert guete = GWert.getNichtErmittelbareGuete(gueteVKfz.getVerfahren());
		try {
			guete = GueteVerfahren.quotient(guetekb, gueteVKfz);
		} catch (GueteException e) {
			Debug.getLogger().error("Guete von " + qb.getName() + " fuer " + //$NON-NLS-1$ //$NON-NLS-2$
					this.prognoseObjekt + " konnte nicht berechnet werden", e); //$NON-NLS-1$
			e.printStackTrace();
		}

		kb.getGueteIndex().setWert(guete.getIndexUnskaliert());

		kb.kopiereInhaltNachModifiziereIndex(zielDatum);
	}

	public void dataRequest(SystemObject object,
			DataDescription dataDescription, byte state) {
	}

	public boolean isRequestSupported(SystemObject object,
			DataDescription dataDescription) {
		return false;
	}

	/**
	 * Abstrakte Methoden
	 */

	/**
	 * Erfragt den Typ dieses Prognoseobjektes (über diesen ist definiert,
	 * welche Parameter-Attributgruppen zur Anwendung kommen)
	 * 
	 * @return der Typ dieses Prognoseobjektes
	 */
	protected abstract PrognoseTyp getPrognoseTyp();

}
