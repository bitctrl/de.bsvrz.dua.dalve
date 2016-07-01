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
package de.bsvrz.dua.dalve.analyse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.prognose.DaMesswertUnskaliert;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.adapter.AbstraktBearbeitungsKnotenAdapter;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVObjektAnmeldung;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.schnittstellen.IDatenFlussSteuerung;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.typen.ModulTyp;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IVerwaltung;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Bekommt die messwertersetzten KZD der Fahrstreifen übergeben und produziert aus Diesen
 * Analysedaten nach den Vorgaben der AFo.<br>
 * (siehe SE-02.00.00.00.00-AFo-4.0 S.115f)
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class FsAnalyseModul extends AbstraktBearbeitungsKnotenAdapter {

	private static final Debug LOGGER = Debug.getLogger();

	/** Datenbeschreibung zur Publikation der Fahrstreifen-Analysedaten. */
	protected static DataDescription PUB_BESCHREIBUNG = null;

	/** Mapt jeden Fahrstreifen auf seine aktuellen Analyse-Parameter. */
	private final Map<SystemObject, AtgVerkehrsDatenKurzZeitAnalyseFs> parameter = new HashMap<>();

	/** Mapt jeden Fahrstreifen auf seinen letzten Analysedatensatz. */
	private final Map<SystemObject, ResultData> fsAufDatenPuffer = new HashMap<>();

	@Override
	public void initialisiere(final IVerwaltung dieVerwaltung) throws DUAInitialisierungsException {
		super.initialisiere(dieVerwaltung);
		setPublikation(true);

		if (PUB_BESCHREIBUNG == null) {
			PUB_BESCHREIBUNG = new DataDescription(
					dieVerwaltung.getVerbindung().getDataModel()
							.getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS),
					dieVerwaltung.getVerbindung().getDataModel()
							.getAspect(DUAKonstanten.ASP_ANALYSE));
		}

		/**
		 * Publikations- und Parameteranmeldungen durchfuehren
		 */
		final Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<>();
		for (final SystemObject fsObj : dieVerwaltung.getSystemObjekte()) {
			parameter.put(fsObj,
					new AtgVerkehrsDatenKurzZeitAnalyseFs(dieVerwaltung.getVerbindung(), fsObj));
			try {
				anmeldungen.add(new DAVObjektAnmeldung(fsObj, PUB_BESCHREIBUNG));
			} catch (final IllegalArgumentException e) {
				throw new DUAInitialisierungsException(Constants.EMPTY_STRING, e);
			}
		}

		getPublikationsAnmeldungen().modifiziereObjektAnmeldung(anmeldungen);
	}

	@Override
	public void aktualisiereDaten(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if (resultat != null) {
					final ResultData analyseDatum = getAnalyseDatum(resultat);
					if (analyseDatum != null) {
						fsAufDatenPuffer.put(resultat.getObject(), analyseDatum);
						getPublikationsAnmeldungen().sende(analyseDatum);
					}
				}
			}
		}
	}

	/**
	 * Berechnet ein Analysedatum eines Fahrstreifens in Bezug auf den übergebenen KZ-Datensatz.
	 *
	 * @param kurzZeitDatum
	 *            ein aktueller KZ-Datensatz
	 * @return ein Analysedatum eines Fahrstreifens in Bezug auf den uebergebenen KZ-Datensatz oder
	 *         <code>null</code>, wenn dieses nicht ermittelt werden konnte
	 */
	private final ResultData getAnalyseDatum(final ResultData kurzZeitDatum) {
		ResultData ergebnis = null;

		if (kurzZeitDatum.getData() != null) {
			final Data analyseDatum = getVerwaltung().getVerbindung()
					.createData(PUB_BESCHREIBUNG.getAttributeGroup());

			analyseDatum.getTimeValue("T").setMillis(//$NON-NLS-1$
					kurzZeitDatum.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$

			/**
			 * Berechne Verkehrsstärken
			 */
			berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qKfz"); //$NON-NLS-1$
			berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qLkw"); //$NON-NLS-1$
			berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qPkw"); //$NON-NLS-1$

			/**
			 * Mittlere Geschwindigkeiten, Belegung und Standardabweichung werden einfach übernommen
			 */
			uebernehmeWert(analyseDatum, kurzZeitDatum, "vKfz"); //$NON-NLS-1$
			uebernehmeWert(analyseDatum, kurzZeitDatum, "vLkw"); //$NON-NLS-1$
			uebernehmeWert(analyseDatum, kurzZeitDatum, "vPkw"); //$NON-NLS-1$
			uebernehmeWert(analyseDatum, kurzZeitDatum, "vgKfz"); //$NON-NLS-1$
			uebernehmeWert(analyseDatum, kurzZeitDatum, "b"); //$NON-NLS-1$
			uebernehmeWert(analyseDatum, kurzZeitDatum, "sKfz"); //$NON-NLS-1$

			/**
			 * Berechne Lkw-Anteil (ab hier sind Analysedaten selbst Eingangswerte)
			 */
			berechneLkwAnteil(analyseDatum, kurzZeitDatum);

			/**
			 * Berechne Dichten
			 */
			berechneDichte(analyseDatum, kurzZeitDatum, "Kfz"); //$NON-NLS-1$
			berechneDichte(analyseDatum, kurzZeitDatum, "Lkw"); //$NON-NLS-1$
			berechneDichte(analyseDatum, kurzZeitDatum, "Pkw"); //$NON-NLS-1$

			/**
			 * Berechne Bemessungsverkehrsstärke
			 */
			berechneBemessungsVerkehrsStaerke(analyseDatum, kurzZeitDatum);

			/**
			 * Berechne Bemessungsdichte
			 */
			berechneBemessungsDichte(analyseDatum, kurzZeitDatum);

			/**
			 * Ergebnisdatensatz
			 */
			ergebnis = new ResultData(kurzZeitDatum.getObject(), PUB_BESCHREIBUNG,
					kurzZeitDatum.getDataTime(), analyseDatum);
		} else {
			final ResultData letzterPublizierterWert = fsAufDatenPuffer
					.get(kurzZeitDatum.getObject());
			if ((letzterPublizierterWert == null) || (letzterPublizierterWert.getData() != null)) {
				ergebnis = new ResultData(kurzZeitDatum.getObject(), PUB_BESCHREIBUNG,
						kurzZeitDatum.getDataTime(), null);
			}
		}

		return ergebnis;
	}

	/**
	 * Berechnet den Lkw-Anteil <code>aLkw</code> (nach SE-02.00.00.00.00-AFo-4.0 S.117f)
	 *
	 * @param analyseDatum
	 *            ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll (in dieses
	 *            Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum
	 *            ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 */
	private final void berechneLkwAnteil(final Data analyseDatum, final ResultData kurzZeitDatum) {
		final DaMesswertUnskaliert qLkw = new DaMesswertUnskaliert("qLkw", analyseDatum); //$NON-NLS-1$
		final DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("qKfz", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert aLkw = new MesswertUnskaliert("aLkw"); //$NON-NLS-1$

		if (qLkw.isFehlerhaftBzwImplausibel() || qKfz.isFehlerhaftBzwImplausibel()) {
			aLkw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			aLkw.getGueteIndex().setWert(0);
		} else {
			if ((qLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
					|| (qKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
				/**
				 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar
				 * gekennzeichnet, wird der Zielwert mit den Statusflags nicht ermittelbar
				 * gekennzeichnet.
				 */
				aLkw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				boolean nichtErmittelbarFehlerhaft = true;

				if (qLkw.getWertUnskaliert() == 0) {
					aLkw.setWertUnskaliert(0);
					nichtErmittelbarFehlerhaft = false;
					if (qLkw.isPlausibilisiert() || qKfz.isPlausibilisiert()) {
						aLkw.setInterpoliert(true);
					}
					GWert qLkwGuete;
					try {
						qLkwGuete = new GWert(analyseDatum, "qLkw"); //$NON-NLS-1$
						final GWert qKfzGuete = new GWert(analyseDatum, "qKfz"); //$NON-NLS-1$
						final GWert aLkwGuete = GueteVerfahren.quotient(qLkwGuete, qKfzGuete);

						aLkw.getGueteIndex().setWert(aLkwGuete.getIndexUnskaliert());
						aLkw.setVerfahren(aLkwGuete.getVerfahren().getCode());
					} catch (final GueteException e) {
						LOGGER.error("Guete-Index fuer aLkw nicht berechenbar in " + kurzZeitDatum, //$NON-NLS-1$
								e);
						e.printStackTrace();
					}
				} else {
					if (qKfz.getWertUnskaliert() > 0) {
						final long aLkwWert = Math.round(((100.0 * (qLkw.getWertUnskaliert()))
								/ (qKfz.getWertUnskaliert())));
						if (DUAUtensilien.isWertInWerteBereich(
								analyseDatum.getItem("aLkw").getItem("Wert"), aLkwWert)) { //$NON-NLS-1$ //$NON-NLS-2$
							aLkw.setWertUnskaliert(aLkwWert);

							GWert qLkwGuete;
							try {
								qLkwGuete = new GWert(analyseDatum, "qLkw"); //$NON-NLS-1$
								final GWert qKfzGuete = new GWert(analyseDatum, "qKfz"); //$NON-NLS-1$
								final GWert aLkwGuete = GueteVerfahren.quotient(qLkwGuete,
										qKfzGuete);

								aLkw.getGueteIndex().setWert(aLkwGuete.getIndexUnskaliert());
								aLkw.setVerfahren(aLkwGuete.getVerfahren().getCode());
							} catch (final GueteException e) {
								LOGGER.error("Guete-Index fuer aLkw nicht berechenbar in " //$NON-NLS-1$
										+ kurzZeitDatum, e);
								e.printStackTrace();
							}

							if (qLkw.isPlausibilisiert() || qKfz.isPlausibilisiert()) {
								aLkw.setInterpoliert(true);
							}
							nichtErmittelbarFehlerhaft = false;
						}
					}
				}

				if (nichtErmittelbarFehlerhaft) {
					aLkw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					aLkw.getGueteIndex().setWert(0);
				}
			}
		}

		aLkw.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Bemessungsverkehrsstärke <code>qB</code> (nach SE-02.00.00.00.00-AFo-4.0
	 * S.117f)
	 *
	 * @param analyseDatum
	 *            ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll (in dieses
	 *            Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum
	 *            ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 */
	private final void berechneBemessungsVerkehrsStaerke(final Data analyseDatum,
			final ResultData kurzZeitDatum) {
		final AtgVerkehrsDatenKurzZeitAnalyseFs parameter = this.parameter
				.get(kurzZeitDatum.getObject());
		final MesswertUnskaliert qB = new MesswertUnskaliert("qB"); //$NON-NLS-1$
		boolean interpoliert = false;

		if (parameter.isInitialisiert()) {
			final double k1 = parameter.getFlk1();
			final double k2 = parameter.getFlk2();

			final DaMesswertUnskaliert vPkw = new DaMesswertUnskaliert("vPkw", analyseDatum); //$NON-NLS-1$
			final DaMesswertUnskaliert vLkw = new DaMesswertUnskaliert("vLkw", analyseDatum); //$NON-NLS-1$
			final DaMesswertUnskaliert qPkw = new DaMesswertUnskaliert("qPkw", analyseDatum); //$NON-NLS-1$
			final DaMesswertUnskaliert qLkw = new DaMesswertUnskaliert("qLkw", analyseDatum); //$NON-NLS-1$

			if (vLkw.isFehlerhaftBzwImplausibel() || qLkw.isFehlerhaftBzwImplausibel()
					|| (vLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
					|| (qLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {

				/**
				 * Also wenn keine Lkw gefahren sind, dann ist qB == qPkw
				 */

				final long qBWert = qPkw.getWertUnskaliert();

				if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("qB").getItem("Wert"), //$NON-NLS-1$ //$NON-NLS-2$
						qBWert)) {
					qB.setWertUnskaliert(qBWert);

					GWert gueteGesamt = GWert.getNichtErmittelbareGuete(
							GueteVerfahren.getZustand(qB.getVerfahren()));

					final GWert qPkwGuete = new GWert(analyseDatum, "qPkw"); //$NON-NLS-1$
					gueteGesamt = qPkwGuete;

					qB.getGueteIndex().setWert(gueteGesamt.getIndexUnskaliert());
					qB.setInterpoliert(qPkw.isInterpoliert());
				} else {
					qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					qB.getGueteIndex().setWert(0);
				}

			} else {
				if (vPkw.isFehlerhaftBzwImplausibel() || qPkw.isFehlerhaftBzwImplausibel()) {
					qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					qB.getGueteIndex().setWert(0);
				} else {
					if ((vPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
							|| (qPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
						qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else {
						GWert guetefL = GWert.getNichtErmittelbareGuete(
								GueteVerfahren.getZustand(qB.getVerfahren()));

						double fL = 0;
						if (vPkw.getWertUnskaliert() > vLkw.getWertUnskaliert()) {
							fL = k1 + (k2 * (vPkw.getWertUnskaliert() - vLkw.getWertUnskaliert()));
							interpoliert |= vPkw.isPlausibilisiert() || vLkw.isPlausibilisiert();
							final GWert vPkwGuete = new GWert(analyseDatum, "vPkw"); //$NON-NLS-1$
							final GWert vLkwGuete = new GWert(analyseDatum, "vLkw"); //$NON-NLS-1$
							try {
								guetefL = GueteVerfahren.differenz(vPkwGuete, vLkwGuete);
							} catch (final GueteException e) {
								LOGGER.error("Guete von fL (qB) konnte nicht ermittelt werden in " //$NON-NLS-1$
										+ kurzZeitDatum, e);
								e.printStackTrace();
							}
						} else {
							fL = k1;
						}
						final long qBWert = Math
								.round(qPkw.getWertUnskaliert() + (fL * qLkw.getWertUnskaliert()));

						if (DUAUtensilien.isWertInWerteBereich(
								analyseDatum.getItem("qB").getItem("Wert"), qBWert)) { //$NON-NLS-1$ //$NON-NLS-2$
							qB.setWertUnskaliert(qBWert);

							GWert gueteGesamt = GWert.getNichtErmittelbareGuete(
									GueteVerfahren.getZustand(qB.getVerfahren()));
							try {
								final GWert qPkwGuete = new GWert(analyseDatum, "qPkw"); //$NON-NLS-1$
								final GWert qLkwGuete = new GWert(analyseDatum, "qLkw"); //$NON-NLS-1$

								// gueteGesamt = GueteVerfahren.summe(qPkwGuete,
								// GueteVerfahren.gewichte(qLkwGuete, fL));
								gueteGesamt = GueteVerfahren.summe(qPkwGuete,
										GueteVerfahren.produkt(qLkwGuete, guetefL));
							} catch (final GueteException e) {
								LOGGER.error("Guete von qB konnte nicht ermittelt werden in " //$NON-NLS-1$
										+ kurzZeitDatum, e);
								e.printStackTrace();
							}

							qB.getGueteIndex().setWert(gueteGesamt.getIndexUnskaliert());
							qB.setInterpoliert(qPkw.isPlausibilisiert() || qLkw.isPlausibilisiert()
									|| interpoliert);
						} else {
							qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							qB.getGueteIndex().setWert(0);
						}
					}
				}
			}

		} else {
			qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			qB.getGueteIndex().setWert(0);
		}

		qB.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Bemessungsdichte <code>kB</code> (nach SE-02.00.00.00.00-AFo-4.0 S.116f)
	 *
	 * @param analyseDatum
	 *            ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll (in dieses
	 *            Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum
	 *            ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 **/
	private final void berechneBemessungsDichte(final Data analyseDatum,
			final ResultData kurzZeitDatum) {
		final DaMesswertUnskaliert vT = new DaMesswertUnskaliert("vKfz", analyseDatum); //$NON-NLS-1$
		final DaMesswertUnskaliert qT = new DaMesswertUnskaliert("qB", analyseDatum); //$NON-NLS-1$

		final MesswertUnskaliert zielK = new MesswertUnskaliert("kB"); //$NON-NLS-1$

		if (vT.isFehlerhaftBzwImplausibel() || qT.isFehlerhaftBzwImplausibel()) {
			zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			zielK.getGueteIndex().setWert(0);
		} else {
			if ((vT.getWertUnskaliert() == 0)
					|| (vT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
				final ResultData analyseTminus1 = fsAufDatenPuffer.get(kurzZeitDatum.getObject());
				if ((analyseTminus1 != null) && (analyseTminus1.getData() != null)) {
					final MesswertUnskaliert kTminus1 = new MesswertUnskaliert("kB", //$NON-NLS-1$
							analyseTminus1.getData());

					final long kTminus1Wert = kTminus1.getWertUnskaliert();
					final AtgVerkehrsDatenKurzZeitAnalyseFs fsParameter = parameter
							.get(kurzZeitDatum.getObject());

					if (!kTminus1.isFehlerhaftBzwImplausibel()) {
						if (fsParameter.isInitialisiert()) {
							long kGrenz = DUAKonstanten.MESSWERT_UNBEKANNT;
							long kMax = DUAKonstanten.MESSWERT_UNBEKANNT;

							kGrenz = fsParameter.getKBGrenz();
							kMax = fsParameter.getKBMax();

							if ((kGrenz != DUAKonstanten.FEHLERHAFT)
									&& (kGrenz != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)
									&& (kMax != DUAKonstanten.FEHLERHAFT)
									&& (kMax != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)) {
								if (kGrenz == DUAKonstanten.NICHT_ERMITTELBAR) {
									zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
								} else {
									if (kTminus1Wert < kGrenz) {
										zielK.setWertUnskaliert(0);
									} else {
										zielK.setWertUnskaliert(kMax);
									}
								}
							}
						}
					} else {
						zielK.setWertUnskaliert(0);
					}
				}
			} else {
				if (qT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					zielK.setWertUnskaliert(Math.round(
							(double) qT.getWertUnskaliert() / (double) vT.getWertUnskaliert()));

					/**
					 * Aenderung analog Mail von Herrn Kappich vom 27.03.08, 1400
					 */
							final AtgVerkehrsDatenKurzZeitAnalyseFs fsParameter = parameter
									.get(kurzZeitDatum.getObject());

					if (fsParameter.isInitialisiert()) {
						final long kMax = fsParameter.getKBMax();

						if (kMax >= 0) {
							if (zielK.getWertUnskaliert() > kMax) {
								zielK.setWertUnskaliert(kMax);
							}
						}
					}

					if (DUAUtensilien.isWertInWerteBereich(
							analyseDatum.getItem("kKfz").getItem("Wert"), //$NON-NLS-1$ //$NON-NLS-2$
							zielK.getWertUnskaliert())) {
						try {
							final GWert qGuete = new GWert(analyseDatum, "qB"); //$NON-NLS-1$
							final GWert vGuete = new GWert(analyseDatum, "vKfz"); //$NON-NLS-1$
							final GWert kGuete = GueteVerfahren.quotient(qGuete, vGuete);
							zielK.getGueteIndex().setWert(kGuete.getIndexUnskaliert());
							zielK.setVerfahren(kGuete.getVerfahren().getCode());
						} catch (final GueteException e) {
							LOGGER.error(
									"Guete-Index fuer kB nicht berechenbar in " + kurzZeitDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}

						if (qT.isPlausibilisiert() || vT.isPlausibilisiert()) {
							zielK.setInterpoliert(true);
						}
					} else {
						zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						zielK.getGueteIndex().setWert(0);
					}
				}
			}

			if (zielK.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
				zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				zielK.getGueteIndex().setWert(0);
			}
		}

		zielK.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Fahrzeugdichte (nach SE-02.00.00.00.00-AFo-4.0 S.116f)
	 *
	 * @param analyseDatum
	 *            ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll (in dieses
	 *            Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum
	 *            ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 * @param fahrZeugKlasse
	 *            eine Fahrzeugklasse, für die der Analysewert ermittelt werden soll
	 */
	private final void berechneDichte(final Data analyseDatum, final ResultData kurzZeitDatum,
			final String fahrZeugKlasse) {
		final DaMesswertUnskaliert vT = new DaMesswertUnskaliert("v" + fahrZeugKlasse, //$NON-NLS-1$
				analyseDatum);
		final DaMesswertUnskaliert qT = new DaMesswertUnskaliert("q" + fahrZeugKlasse, //$NON-NLS-1$
				analyseDatum);

		final MesswertUnskaliert zielK = new MesswertUnskaliert("k" + fahrZeugKlasse); //$NON-NLS-1$

		if ((vT.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)
				&& (qT.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)) {

			if ((vT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
					|| (vT.getWertUnskaliert() == 0)) {

				final ResultData analyseTminus1 = fsAufDatenPuffer.get(kurzZeitDatum.getObject());
				if ((analyseTminus1 != null) && (analyseTminus1.getData() != null)) {
					final MesswertUnskaliert kTminus1 = new MesswertUnskaliert("k" + fahrZeugKlasse, //$NON-NLS-1$
							analyseTminus1.getData());

					final long kTminus1Wert = kTminus1.getWertUnskaliert();

					if (!kTminus1.isImplausibel() && (kTminus1.getWertUnskaliert() >= 0)) {
						final AtgVerkehrsDatenKurzZeitAnalyseFs fsParameter = parameter
								.get(kurzZeitDatum.getObject());
						if (fsParameter.isInitialisiert()) {
							long kGrenz = DUAKonstanten.MESSWERT_UNBEKANNT;
							long kMax = DUAKonstanten.MESSWERT_UNBEKANNT;

							if (fahrZeugKlasse.startsWith("K")) { // Kfz //$NON-NLS-1$
								kGrenz = fsParameter.getKKfzGrenz();
								kMax = fsParameter.getKKfzMax();
							} else if (fahrZeugKlasse.startsWith("L")) { // Lkw //$NON-NLS-1$
								kGrenz = fsParameter.getKLkwGrenz();
								kMax = fsParameter.getKLkwMax();
							} else { // Pkw
								kGrenz = fsParameter.getKPkwGrenz();
								kMax = fsParameter.getKPkwMax();
							}

							if ((kGrenz != DUAKonstanten.FEHLERHAFT)
									&& (kGrenz != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)
									&& (kMax != DUAKonstanten.FEHLERHAFT)
									&& (kMax != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)) {
								if (kGrenz == DUAKonstanten.NICHT_ERMITTELBAR) {
									zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
								} else {
									if (kTminus1Wert < kGrenz) {
										zielK.setWertUnskaliert(0);
									} else {
										zielK.setWertUnskaliert(kMax);
									}
								}
							}
						}
					} else {
						/**
						 * offensichtlich laut Prüfdaten:
						 */
						// zielK.setWertUnskaliert(0);
						zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
						zielK.getGueteIndex().setWert(0);
					}
				}
			} else {
				if (qT.isFehlerhaftBzwImplausibel()) {
					zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					zielK.getGueteIndex().setWert(0);
				} else {

					if (qT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
						zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else {
						zielK.setWertUnskaliert(Math.round(
								(double) qT.getWertUnskaliert() / (double) vT.getWertUnskaliert()));

						/**
						 * Aenderung analog Mail von Herrn Kappich vom 27.03.08, 1400
						 */
								final AtgVerkehrsDatenKurzZeitAnalyseFs fsParameter = parameter
										.get(kurzZeitDatum.getObject());
						if (fsParameter.isInitialisiert()) {
							long kGrenz = DUAKonstanten.MESSWERT_UNBEKANNT;
							long kMax = DUAKonstanten.MESSWERT_UNBEKANNT;

							if (fahrZeugKlasse.startsWith("K")) { // Kfz //$NON-NLS-1$
								kGrenz = fsParameter.getKKfzGrenz();
								kMax = fsParameter.getKKfzMax();
							} else if (fahrZeugKlasse.startsWith("L")) { // Lkw //$NON-NLS-1$
								kGrenz = fsParameter.getKLkwGrenz();
								kMax = fsParameter.getKLkwMax();
							} else { // Pkw
								kGrenz = fsParameter.getKPkwGrenz();
								kMax = fsParameter.getKPkwMax();
							}

							if ((kGrenz != DUAKonstanten.FEHLERHAFT)
									&& (kGrenz != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)
									&& (kMax != DUAKonstanten.FEHLERHAFT)
									&& (kMax != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT)) {
								if (zielK.getWertUnskaliert() > kMax) {
									zielK.setWertUnskaliert(kMax);
								}
							}
						}

						if (DUAUtensilien.isWertInWerteBereich(
								analyseDatum.getItem("k" + fahrZeugKlasse).getItem("Wert"), //$NON-NLS-1$ //$NON-NLS-2$
								zielK.getWertUnskaliert())) {
							try {
								final GWert qGuete = new GWert(analyseDatum, "q" + fahrZeugKlasse); //$NON-NLS-1$
								final GWert vGuete = new GWert(analyseDatum, "v" + fahrZeugKlasse); //$NON-NLS-1$
								final GWert kGuete = GueteVerfahren.quotient(qGuete, vGuete);
								zielK.getGueteIndex().setWert(kGuete.getIndexUnskaliert());
								zielK.setVerfahren(kGuete.getVerfahren().getCode());
							} catch (final GueteException e) {
								LOGGER.error("Guete-Index fuer k" + fahrZeugKlasse + //$NON-NLS-1$
										" nicht berechenbar in " + kurzZeitDatum, e); //$NON-NLS-1$
								e.printStackTrace();
							}

							if (qT.isPlausibilisiert() || vT.isPlausibilisiert()) {
								zielK.setInterpoliert(true);
							}
						} else {
							zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							zielK.getGueteIndex().setWert(0);
						}
					}
				}
			}
		}

		if (zielK.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
			zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			zielK.getGueteIndex().setWert(0);
		}

		zielK.kopiereInhaltNachModifiziereIndex(analyseDatum);

		// if(vT.isFehlerhaftBzwImplausibel() ||
		// qT.isFehlerhaftBzwImplausibel()){
		// zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		// }else{
		// if(vT.getWertUnskaliert() == 0 ||
		// vT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
		//
		// ResultData analyseTminus1 = this.fsAufDatenPuffer.get(kurzZeitDatum.getObject());
		// if(analyseTminus1 != null &&
		// analyseTminus1.getData() != null){
		// MesswertUnskaliert kTminus1 = new MesswertUnskaliert("k" + fahrZeugKlasse, //$NON-NLS-1$
		// analyseTminus1.getData());
		//
		// long kTminus1Wert = kTminus1.getWertUnskaliert();
		//
		// if(!kTminus1.isFehlerhaftBzwImplausibel()){
		// AtgVerkehrsDatenKurzZeitAnalyseFs fsParameter =
		// this.parameter.get(kurzZeitDatum.getObject());
		// if(fsParameter.isInitialisiert()){
		// long kGrenz = DUAKonstanten.MESSWERT_UNBEKANNT;
		// long kMax = DUAKonstanten.MESSWERT_UNBEKANNT;
		//
		// if(fahrZeugKlasse.startsWith("K")){ // Kfz //$NON-NLS-1$
		// kGrenz = fsParameter.getKKfzGrenz();
		// kMax = fsParameter.getKKfzMax();
		// }else
		// if(fahrZeugKlasse.startsWith("L")){ // Lkw //$NON-NLS-1$
		// kGrenz = fsParameter.getKLkwGrenz();
		// kMax = fsParameter.getKLkwMax();
		// }else{ // Pkw
		// kGrenz = fsParameter.getKPkwGrenz();
		// kMax = fsParameter.getKPkwMax();
		// }
		//
		// if(kGrenz != DUAKonstanten.FEHLERHAFT &&
		// kGrenz != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT &&
		// kMax != DUAKonstanten.FEHLERHAFT &&
		// kMax != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT){
		// if(kGrenz == DUAKonstanten.NICHT_ERMITTELBAR){
		// zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		// }else{
		// if(kTminus1Wert < kGrenz){
		// zielK.setWertUnskaliert(0);
		// }else{
		// zielK.setWertUnskaliert(kMax);
		// }
		// }
		// }
		// }
		// }
		// }
		// }else{
		//
		// if(qT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
		// zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		// }else{
		// zielK.setWertUnskaliert(Math.round((double)qT.getWertUnskaliert() /
		// (double)vT.getWertUnskaliert()));
		//
		// if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("k" +
		// fahrZeugKlasse).getItem("Wert"), zielK.getWertUnskaliert())){ //$NON-NLS-1$ //$NON-NLS-2$
		// try {
		// GWert qGuete = new GWert(analyseDatum, "q" + fahrZeugKlasse); //$NON-NLS-1$
		// GWert vGuete = new GWert(analyseDatum, "v" + fahrZeugKlasse); //$NON-NLS-1$
		// GWert kGuete = GueteVerfahren.quotient(qGuete, vGuete);
		// zielK.getGueteIndex().setWert(kGuete.getIndexUnskaliert());
		// zielK.setVerfahren(kGuete.getVerfahren().getCode());
		// } catch (GueteException e) {
		// LOGGER.error("Guete-Index fuer k" + fahrZeugKlasse + //$NON-NLS-1$
		// " nicht berechenbar in " + kurzZeitDatum, e); //$NON-NLS-1$
		// e.printStackTrace();
		// }
		//
		// if(qT.isPlausibilisiert() || vT.isPlausibilisiert()){
		// zielK.setInterpoliert(true);
		// }
		// }else{
		// zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		// }
		// }
		// }
		//
		// if(zielK.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT){
		// zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		// }
		// }
		//
		// zielK.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet die Verkehrsstärken analog SE-02.00.00.00.00-AFo-4.0 S.116f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param kurzZeitDatum
	 *            das Roh-KZ-Datum
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	private final void berechneVerkehrsStaerke(final Data analyseDatum,
			final ResultData kurzZeitDatum, final String attName) {
		final long TinS = kurzZeitDatum.getData().getTimeValue("T").getMillis() / 1000; //$NON-NLS-1$
		final DaMesswertUnskaliert qMwe = new DaMesswertUnskaliert(attName,
				kurzZeitDatum.getData());
		final MesswertUnskaliert qAnalyse = new MesswertUnskaliert(attName,
				kurzZeitDatum.getData());

		if (qMwe.isFehlerhaftBzwImplausibel()) {
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			qAnalyse.getGueteIndex().setWert(0);
		} else {
			/**
			 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar
			 * gekennzeichnet, wird der Zielwert mit den Statusflags nicht ermittelbar
			 * gekennzeichnet.
			 */
			if (qMwe.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				boolean nichtErmittelbarFehlerhaft = true;
				if (TinS > 0) {
					final long q = Math.round(((qMwe.getWertUnskaliert() * 3600.0) / TinS));
					if (DUAUtensilien.isWertInWerteBereich(
							analyseDatum.getItem(attName).getItem("Wert"), q)) { //$NON-NLS-1$
						qAnalyse.setWertUnskaliert(q);
						qAnalyse.setGueteIndex(qMwe.getGueteIndex());
						qAnalyse.setVerfahren(qMwe.getVerfahren());

						if (qMwe.isPlausibilisiert()) {
							qAnalyse.setInterpoliert(true);
							qAnalyse.setFormalMax(false);
							qAnalyse.setFormalMin(false);
							qAnalyse.setLogischMax(false);
							qAnalyse.setLogischMin(false);
						}
						nichtErmittelbarFehlerhaft = false;
					}
				}

				if (nichtErmittelbarFehlerhaft) {
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					qAnalyse.getGueteIndex().setWert(0);
				}
			}
		}

		qAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Übernimmt einen Wert aus dem Kurzzeitdatum in das Analysedatum.
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param kurzZeitDatum
	 *            das Roh-KZ-Datum
	 * @param attName
	 *            der Attributname des Verkehrswertes, der übernommen werden soll
	 */
	private final void uebernehmeWert(final Data analyseDatum, final ResultData kurzZeitDatum,
			final String attName) {
		final DaMesswertUnskaliert mweWert = new DaMesswertUnskaliert(attName,
				kurzZeitDatum.getData());
		final MesswertUnskaliert analyseWert = new MesswertUnskaliert(attName,
				kurzZeitDatum.getData());

		if (mweWert.isFehlerhaftBzwImplausibel()) {
			analyseWert.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			analyseWert.getGueteIndex().setWert(0);
		} else {
			/**
			 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar
			 * gekennzeichnet, wird der Zielwert mit den Statusflags nicht ermittelbar
			 * gekennzeichnet.
			 */
			if (mweWert.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				mweWert.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				analyseWert.setWertUnskaliert(mweWert.getWertUnskaliert());
				analyseWert.setGueteIndex(mweWert.getGueteIndex());
				analyseWert.setVerfahren(mweWert.getVerfahren());
				if (mweWert.isPlausibilisiert()) {
					analyseWert.setInterpoliert(true);
					analyseWert.setFormalMax(false);
					analyseWert.setFormalMin(false);
					analyseWert.setLogischMax(false);
					analyseWert.setLogischMin(false);
				}
			}
		}

		// /**
		// * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar
		// gekennzeichnet, wird
		// * der Zielwert mit den Statusflags nicht ermittelbar gekennzeichnet.
		// */
		// if(mweWert.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
		// mweWert.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		// }else{
		// analyseWert.setWertUnskaliert(mweWert.getWertUnskaliert());
		// analyseWert.setGueteIndex(mweWert.getGueteIndex());
		// analyseWert.setVerfahren(mweWert.getVerfahren());
		// if(mweWert.isPlausibilisiert()){
		// analyseWert.setInterpoliert(true);
		// analyseWert.setFormalMax(false);
		// analyseWert.setFormalMin(false);
		// analyseWert.setLogischMax(false);
		// analyseWert.setLogischMin(false);
		// }
		// }
		//
		if (mweWert.isFehlerhaftBzwImplausibel()) {
			analyseWert.getGueteIndex().setWert(0);
		}

		analyseWert.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	@Override
	public ModulTyp getModulTyp() {
		return null;
	}

	@Override
	public void aktualisierePublikation(final IDatenFlussSteuerung dfs) {
		// hier wird nicht dynamisch publiziert
	}

}
