/*
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007-2015 BitCtrl Systems GmbH
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
package de.bsvrz.dua.dalve.prognose;

import java.util.HashSet;
import java.util.Set;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.DataNotSubscribedException;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SendSubscriptionNotConfirmed;
import de.bsvrz.dav.daf.main.SenderRole;
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

// TODO: Auto-generated Javadoc
/**
 * Ueber dieses Objekt werden die Prognosedaten fuer <b>einen</b> Fahrstreifen oder einen
 * Messquerschnitt erstellt/publiziert bzw. deren Erstellung verwaltet
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public abstract class AbstraktPrognoseObjekt implements ClientReceiverInterface,
		ClientSenderInterface {

	private static final Debug LOGGER = Debug.getLogger();

	/** Verbindung zum Datenverteiler. */
	private static ClientDavInterface DAV = null;

	/**
	 * Die Parameter, die die Erstellung der Daten steuern (alpha, beta, etc.)
	 */
	private AtgPrognoseParameter parameter = null;

	/**
	 * Das Objekt, fuer das Prognosedaten und geglaettete Daten erstellt werden sollen (Fahrstreifen
	 * oder Messquerschnitt).
	 */
	private SystemObject prognoseObjekt = null;

	/** Publikationsbeschreibung der geglaetteten Daten. */
	private DataDescription pubBeschreibungGlatt = null;

	/** Publikationsbeschreibung der Prognosedaten. */
	private DataDescription pubBeschreibungPrognose = null;

	/** zeigt an, ob dieses Objekt im Moment auf keine Daten steht. */
	private boolean aktuellKeineDaten = true;

	/** Sendesteuerung fuer Prognosedaten. */
	private boolean sendePrognoseDaten = false;

	/** Sendesteuerung fuer geglaettete Daten. */
	private boolean sendeGeglaetteteDaten = false;

	/** Fuer jedes zu berechnende Attribut (des Zieldatums) ein Prognoseobjekt. */
	private final Set<DavAttributPrognoseObjekt> attributePuffer = new HashSet<DavAttributPrognoseObjekt>();

	/**
	 * Initialisiert dieses Objekt. Nach Beendigung dieser Methode empfängt und publiziert dieses
	 * Objekt Daten
	 *
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 * @param prognoseObjekt
	 *            das Prognoseobjekt, für das prognostiziert werden soll
	 * @throws DUAInitialisierungsException
	 *             wenn die Sendeanmeldung fehlschlaegt
	 */
	public final void initialisiere(final ClientDavInterface dav, final SystemObject prognoseObjekt)
					throws DUAInitialisierungsException {
		if (DAV == null) {
			DAV = dav;
		}
		this.prognoseObjekt = prognoseObjekt;

		/**
		 * Auf Parameter anmelden
		 */
		parameter = new AtgPrognoseParameter(DAV, prognoseObjekt, getPrognoseTyp());

		/**
		 * Alle Prognoseattribute initialisieren
		 */
		for (final PrognoseAttribut attribut : PrognoseAttribut.getInstanzen()) {
			final DavAttributPrognoseObjekt attributPrognose = new DavAttributPrognoseObjekt(
					prognoseObjekt, attribut, getPrognoseTyp());
			parameter.addListener(attributPrognose, attribut);
			attributePuffer.add(attributPrognose);
		}

		/**
		 * Sendeanmeldungen fuer Prognosewerte und geglaetttete Werte
		 */
		pubBeschreibungPrognose = new DataDescription(
				DatenaufbereitungLVE.getPubAtgPrognose(prognoseObjekt), getPrognoseTyp()
						.getAspekt());
		pubBeschreibungGlatt = new DataDescription(
				DatenaufbereitungLVE.getPubAtgGlatt(prognoseObjekt), getPrognoseTyp().getAspekt());
		try {
			DAV.subscribeSender(this, prognoseObjekt, pubBeschreibungPrognose, SenderRole.source());
			DAV.subscribeSender(this, prognoseObjekt, pubBeschreibungGlatt, SenderRole.source());
		} catch (final OneSubscriptionPerSendData e) {
			throw new DUAInitialisierungsException(Constants.EMPTY_STRING, e);
		}

		/**
		 * Impliziter Start des Objektes: Anmeldung auf Daten
		 */
		DAV.subscribeReceiver(this, prognoseObjekt,
				new DataDescription(DatenaufbereitungLVE.getAnalyseAtg(prognoseObjekt), DAV
				.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)), ReceiveOptions
						.normal(), ReceiverRole.receiver());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if (resultat != null) {
					boolean datenSenden = true;
					Data glaettungNutzdaten = null;
					Data prognoseNutzdaten = null;

					try {
						for (final DavAttributPrognoseObjekt attribut : attributePuffer) {
							attribut.aktualisiere(resultat);
						}

						if (resultat.getData() != null) {
							aktuellKeineDaten = false;

							/**
							 * Baue geglaetteten Ergebniswert zusammen:
							 */
							glaettungNutzdaten = DAV.createData(DatenaufbereitungLVE
									.getPubAtgGlatt(prognoseObjekt));
							for (final DavAttributPrognoseObjekt attribut : attributePuffer) {
								attribut.exportiereDatenGlatt(glaettungNutzdaten);
							}
							fuegeKBPHinzu(glaettungNutzdaten, false, attributePuffer);

							/**
							 * Baue Prognosewert zusammen:
							 */
							prognoseNutzdaten = DAV.createData(DatenaufbereitungLVE
									.getPubAtgPrognose(prognoseObjekt));
							for (final DavAttributPrognoseObjekt attribut : attributePuffer) {
								attribut.exportiereDatenPrognose(prognoseNutzdaten);
							}
							fuegeKBPHinzu(prognoseNutzdaten, true, attributePuffer);

							if (prognoseObjekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
								final long T = resultat.getData().getTimeValue("T").getMillis(); //$NON-NLS-1$
								glaettungNutzdaten.getTimeValue("T").setMillis(T); //$NON-NLS-1$
								prognoseNutzdaten.getTimeValue("T").setMillis(T); //$NON-NLS-1$
							}

						} else {
							if (aktuellKeineDaten) {
								datenSenden = false;
							}
							aktuellKeineDaten = true;
						}
					} catch (final PrognoseParameterException e) {
						LOGGER.warning("Prognosedaten koennen fuer " + prognoseObjekt //$NON-NLS-1$ 
								+ " nicht berechnet werden:\n" + e.getMessage()); //$NON-NLS-1$
						if (aktuellKeineDaten) {
							datenSenden = false;
						}
						aktuellKeineDaten = true;
					}

					if (datenSenden) {
						final ResultData glaettungsDatum = new ResultData(prognoseObjekt,
								pubBeschreibungGlatt, resultat.getDataTime(), glaettungNutzdaten);
						final ResultData prognoseDatum = new ResultData(prognoseObjekt,
								pubBeschreibungPrognose, resultat.getDataTime(), prognoseNutzdaten);

						try {
							if (sendeGeglaetteteDaten) {
								DAV.sendData(glaettungsDatum);
							} else {
								LOGGER.fine("Geglaettete Daten fuer " + prognoseObjekt + //$NON-NLS-1$
										" koennen nicht versendet werden (Kein Abnehmer)"); //$NON-NLS-1$
							}
						} catch (final DataNotSubscribedException e) {
							LOGGER
							.error("Geglaettete Daten konnten nicht gesendet werden: " + glaettungsDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						} catch (final SendSubscriptionNotConfirmed e) {
							LOGGER
							.error("Geglaettete Daten konnten nicht gesendet werden: " + glaettungsDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}

						try {
							if (sendePrognoseDaten) {
								DAV.sendData(prognoseDatum);
							} else {
								LOGGER.fine("Prognosedaten fuer " + prognoseObjekt + //$NON-NLS-1$
										" koennen nicht versendet werden (Kein Abnehmer)"); //$NON-NLS-1$
							}
						} catch (final DataNotSubscribedException e) {
							LOGGER
							.error("Prognosedaten konnten nicht gesendet werden: " + prognoseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						} catch (final SendSubscriptionNotConfirmed e) {
							LOGGER
							.error("Prognosedaten konnten nicht gesendet werden: " + prognoseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * Fuegt einem errechneten Prognosedatum bzw. einem geglaetteten Datum den Wert
	 * <code>k(K)BP</code> bzw. <code>k(K)BG</code> hinzu
	 *
	 * @param zielDatum
	 *            ein veraenderbares Zieldatum der Attributgruppe
	 * @param prognose
	 *            ob ein Prognosewert veraendert werden soll
	 * @param attributPuffer
	 *            ungerundete Prognosewerte
	 */
	private final void fuegeKBPHinzu(final Data zielDatum, final boolean prognose,
			final Set<DavAttributPrognoseObjekt> attributPuffer) {
		DaMesswertUnskaliert qb = null;
		double qBOrig = 0;
		DaMesswertUnskaliert vKfz = null;
		double vKfzOrig = 0;
		MesswertUnskaliert kb = null;
		GWert gueteVKfz = null;
		GWert guetekb = null;

		for (final DavAttributPrognoseObjekt attribut : attributPuffer) {
			if (prognose) {
				if (attribut.getAttribut().equals(PrognoseAttribut.QB)) {
					// qBOrig = attribut.getZPOriginal();
					qBOrig = attribut.getZP();
				}
				if (attribut.getAttribut().equals(PrognoseAttribut.V_KFZ)) {
					// vKfzOrig = attribut.getZPOriginal();
					vKfzOrig = attribut.getZP();
				}
			} else {
				if (attribut.getAttribut().equals(PrognoseAttribut.QB)) {
					qBOrig = attribut.getZGOriginal();
				}
				if (attribut.getAttribut().equals(PrognoseAttribut.V_KFZ)) {
					vKfzOrig = attribut.getZGOriginal();
				}
			}

		}

		if (prognose) {
			qb = new DaMesswertUnskaliert(
					PrognoseAttribut.QB.getAttributNamePrognose(prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)), zielDatum);
			vKfz = new DaMesswertUnskaliert(
					PrognoseAttribut.V_KFZ.getAttributNamePrognose(prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)), zielDatum);
			kb = new MesswertUnskaliert(PrognoseAttribut.KB.getAttributNamePrognose(prognoseObjekt
					.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			guetekb = new GWert(zielDatum,
					PrognoseAttribut.QB.getAttributNamePrognose(prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			gueteVKfz = new GWert(zielDatum,
					PrognoseAttribut.V_KFZ.getAttributNamePrognose(prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
		} else {
			qb = new DaMesswertUnskaliert(PrognoseAttribut.QB.getAttributNameGlatt(prognoseObjekt
					.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)), zielDatum);
			vKfz = new DaMesswertUnskaliert(
					PrognoseAttribut.V_KFZ.getAttributNameGlatt(prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)), zielDatum);
			kb = new MesswertUnskaliert(PrognoseAttribut.KB.getAttributNameGlatt(prognoseObjekt
					.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			guetekb = new GWert(zielDatum, PrognoseAttribut.QB.getAttributNameGlatt(prognoseObjekt
					.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
			gueteVKfz = new GWert(zielDatum,
					PrognoseAttribut.V_KFZ.getAttributNameGlatt(prognoseObjekt
							.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)));
		}

		kb.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		if (vKfz.getWertUnskaliert() > 0) {
			if (qb.getWertUnskaliert() >= 0) {
				kb.setWertUnskaliert(Math.round(qBOrig / vKfzOrig));
				if (DUAUtensilien.isWertInWerteBereich(
						zielDatum.getItem(kb.getName()).getItem("Wert"), kb.getWertUnskaliert())) { //$NON-NLS-1$
					kb.setWertUnskaliert(kb.getWertUnskaliert());
					kb.setInterpoliert(qb.isPlausibilisiert() || vKfz.isPlausibilisiert());
				}
			}
		}

		GWert guete = GWert.getNichtErmittelbareGuete(gueteVKfz.getVerfahren());
		try {
			guete = GueteVerfahren.quotient(guetekb, gueteVKfz);
		} catch (final GueteException e) {
			LOGGER.error("Guete von " + qb.getName() + " fuer " + //$NON-NLS-1$ //$NON-NLS-2$
					prognoseObjekt + " konnte nicht berechnet werden", e); //$NON-NLS-1$
			e.printStackTrace();
		}

		kb.getGueteIndex().setWert(guete.getIndexUnskaliert());

		kb.kopiereInhaltNachModifiziereIndex(zielDatum);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dataRequest(final SystemObject object, final DataDescription dataDescription,
			final byte state) {
		if (dataDescription.getAttributeGroup().equals(pubBeschreibungGlatt.getAttributeGroup())) {
			sendeGeglaetteteDaten = state == ClientSenderInterface.START_SENDING;
		} else if (dataDescription.getAttributeGroup().equals(
				pubBeschreibungPrognose.getAttributeGroup())) {
			sendePrognoseDaten = state == ClientSenderInterface.START_SENDING;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRequestSupported(final SystemObject object,
			final DataDescription dataDescription) {
		return true;
	}

	/**
	 * Abstrakte Methoden.
	 *
	 * @return the prognose typ
	 */

	/**
	 * Erfragt den Typ dieses Prognoseobjektes (über diesen ist definiert, welche
	 * Parameter-Attributgruppen zur Anwendung kommen)
	 *
	 * @return der Typ dieses Prognoseobjektes
	 */
	protected abstract PrognoseTyp getPrognoseTyp();

}
