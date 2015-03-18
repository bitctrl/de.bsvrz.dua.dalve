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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.dalve.ErfassungsIntervallDauerMQ;
import de.bsvrz.dua.dalve.prognose.DaMesswertUnskaliert;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.FahrStreifen;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * In diesem Objekt werden alle aktuellen Werte die zur Berechnung der Analysewerte eines
 * Messquerschnitts notwendig sind gespeichert. Jedes mit dem MQ assoziierte Fahrstreifendatum muss
 * durch dieses Objekt (Methode <code>trigger(..)</code>) geleitet werden um ggf. auch eine neue
 * Berechnung von Analysewerten auszulösen.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 * @version $Id$
 */
public class DaAnalyseMessQuerschnitt implements ClientReceiverInterface {

	private static final Debug LOGGER = Debug.getLogger();

	/**
	 * Verbindung zum Analysemodul.
	 */
	protected static MqAnalyseModul mqAnalyse = null;

	/**
	 * der mit diesem Objekt assoziierte Messquerschnitt.
	 */
	protected SystemObject messQuerschnitt = null;

	/**
	 * letztes für diesen Messquerschnitt errechnetes (veröffentlichtes) Ergebnis.
	 */
	protected ResultData letztesErgebnis = null;

	/**
	 * Aktuelle Analyseparameter dieses MQs.
	 */
	protected AtgVerkehrsDatenKurzZeitAnalyseMq parameter = null;

	/**
	 * Mapt alle hier betrachteten Fahrstreifen auf das letzte von ihnen empfangene Analysedatum.
	 */
	private final Map<SystemObject, ResultData> aktuelleFSAnalysen = new HashMap<SystemObject, ResultData>();

	/**
	 * Alle aktuellen Fahrstreifenanalysen mit Nutzdaten.
	 */
	private final Map<SystemObject, ResultData> aktuelleFSAnalysenNutz = new HashMap<SystemObject, ResultData>();

	/**
	 * Tracker fuer die Erfassungsintervalldauer des MQ.
	 */
	private ErfassungsIntervallDauerMQ mqT = null;

	/**
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zurück. Nach dieser
	 * Initialisierung ist das Objekt auf alle Daten (seiner assoziierten Fahrstreifen) angemeldet
	 * und analysiert ggf. Daten
	 *
	 * @param analyseModul
	 *            Verbindung zum Analysemodul
	 * @param messQuerschnitt1
	 *            der Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException
	 *             wenn die Konfigurationsdaten des MQs nicht vollständig ausgelesen werden konnte
	 */
	public DaAnalyseMessQuerschnitt initialisiere(final MqAnalyseModul analyseModul,
			final SystemObject messQuerschnitt1) throws DUAInitialisierungsException {
		if (mqAnalyse == null) {
			mqAnalyse = analyseModul;
		}

		mqT = ErfassungsIntervallDauerMQ.getInstanz(mqAnalyse.getDav(), messQuerschnitt1);

		if (mqT == null) {
			throw new RuntimeException("Erfassungsintervalldauer von MQ " + messQuerschnitt1
					+ " kann nicht ermittelt werden.");
		}

		messQuerschnitt = messQuerschnitt1;

		if (MessQuerschnitt.getInstanz(messQuerschnitt1) != null) {
			for (final FahrStreifen fs : MessQuerschnitt.getInstanz(messQuerschnitt1)
					.getFahrStreifen()) {
				aktuelleFSAnalysen.put(fs.getSystemObject(), null);
			}
		} else {
			throw new DUAInitialisierungsException("MQ-Konfiguration von " + messQuerschnitt1 + //$NON-NLS-1$
					" konnte nicht vollstaendig ausgelesen werden"); //$NON-NLS-1$
		}

		if (aktuelleFSAnalysen.keySet().isEmpty()) {
			LOGGER.warning("Der MQ " + messQuerschnitt + " hat keine Fahrstreifen"); //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}

		/**
		 * Anmeldung auf Parameter und alle Daten der assoziierten Messquerschnitte
		 */
		parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(mqAnalyse.getDav(), messQuerschnitt1);
		mqAnalyse.getDav().subscribeReceiver(
				this,
				aktuelleFSAnalysen.keySet(),
				new DataDescription(mqAnalyse.getDav().getDataModel()
						.getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS), mqAnalyse.getDav()
						.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)),
						ReceiveOptions.normal(), ReceiverRole.receiver());

		return this;
	}

	/**
	 * Dieser Methode sollten alle aktuellen Daten für alle mit diesem Messquerschnitt assoziierten
	 * Fahrstreifen uebergeben werden. Ggf. wird dadurch dann eine Berechnung der Analysewerte
	 * dieses Messquerschnittes ausgeluest.
	 *
	 * @param triggerDatum
	 *            ein KZ-Datum eines assoziierten Fahrstreifens
	 * @return ein Analysedatum fuer diesen Messquerschnitt, wenn das <code>triggerDatum</code> eine
	 *         Berechnung ausgeloest hat, oder <code>null</code> sonst
	 */
	protected ResultData trigger(final ResultData triggerDatum) {
		ResultData ergebnis = null;
		aktuelleFSAnalysen.put(triggerDatum.getObject(), triggerDatum);

		/**
		 * Ein Analysedatum fuer den Fahrstreifen soll dann berechnet werden, wenn für alle
		 * Fahrstreifen, welche Nutzdaten haben (aber mindestenes einer) ein Datum mit dem gleichen
		 * Zeitstempel gekommen ist.
		 */
		aktuelleFSAnalysenNutz.clear();
		boolean berechne = false;
		long zeitStempel = -1;
		for (final SystemObject fs : aktuelleFSAnalysen.keySet()) {
			final ResultData fsDatum = aktuelleFSAnalysen.get(fs);

			if (fsDatum != null) {
				if (fsDatum.getData() != null) {
					aktuelleFSAnalysenNutz.put(fsDatum.getObject(), fsDatum);
					if (zeitStempel == -1) {
						/**
						 * erstes Datum
						 */
						zeitStempel = fsDatum.getDataTime();
						berechne = true;
					} else {
						/**
						 * Fuer den Fall, dass die Zeitstempel der Daten nicht uebereinstimmen, wird
						 * keine Daten veroeffentlicht
						 */
						if (fsDatum.getDataTime() != zeitStempel) {
							ergebnis = new ResultData(messQuerschnitt,
									MqAnalyseModul.pubBeschreibung, System.currentTimeMillis(),
									null);
							berechne = false;
							break;
						}
					}
				} else {
					/**
					 * Wenn fuer mindestens einen Fahrstreifen keine Nutzdaten vorliegen, dann
					 * veroeffentliche <code>keine Daten</code> fuer den Messquerschnitt
					 */
					ergebnis = new ResultData(messQuerschnitt, MqAnalyseModul.pubBeschreibung,
							System.currentTimeMillis(), null);
					berechne = false;
					break;
				}
			} else {
				/**
				 * Wenn nicht fuer ALLE Fahrstreifen des Messquerschnittes ein Datensatz vorliegt,
				 * dann mache nichts
				 */
				berechne = false;
				break;
			}
		}

		if (berechne) {
			final long datenZeit = zeitStempel;
			final Data analyseDatum = mqAnalyse.getDav().createData(
					MqAnalyseModul.pubBeschreibung.getAttributeGroup());

			if (mqT.getT() != ErfassungsIntervallDauerMQ.NICHT_EINHEITLICH) {

				/**
				 * Berechne Verkehrsstärken
				 */
				berechneVerkehrsStaerke(analyseDatum, "Kfz"); //$NON-NLS-1$
				berechneVerkehrsStaerke(analyseDatum, "Lkw"); //$NON-NLS-1$
				berechneVerkehrsStaerke(analyseDatum, "Pkw"); //$NON-NLS-1$

				/**
				 * Berechne mittlere Geschwindigkeiten
				 */
				berechneMittlereGeschwindigkeiten(analyseDatum, "Kfz", "V", "v"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				berechneMittlereGeschwindigkeiten(analyseDatum, "Lkw", "V", "v"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				berechneMittlereGeschwindigkeiten(analyseDatum, "Pkw", "V", "v"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				berechneMittlereGeschwindigkeiten(analyseDatum, "Kfz", "Vg", "vg"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

				/**
				 * Belegung B und BMax
				 */
				berechneBelegung(analyseDatum);

				/**
				 * Standardabweichung
				 */
				berechneStandardabweichung(analyseDatum);

				/**
				 * Berechne LKW-Anteil
				 */
				berechneLkwAnteil(analyseDatum);

				/**
				 * Berechne Fahrzeugdichten
				 */
				berechneDichte(analyseDatum, "Kfz"); //$NON-NLS-1$
				berechneDichte(analyseDatum, "Lkw"); //$NON-NLS-1$
				berechneDichte(analyseDatum, "Pkw"); //$NON-NLS-1$

				/**
				 * Bemessungsverkehrsstärke
				 */
				berechneBemessungsVerkehrsstaerke(analyseDatum);

				/**
				 * Bemessungsdichte
				 */
				berechneBemessungsdichte(analyseDatum);

				/**
				 * Berechne die gewichtete Differenzgeschwindigkeit im Messquerschnitt
				 */
				berechneVDifferenz(analyseDatum);
			}

			ergebnis = new ResultData(messQuerschnitt, MqAnalyseModul.pubBeschreibung, datenZeit,
					analyseDatum);

			/**
			 * Puffer wieder zurücksetzen
			 */
			for (final SystemObject obj : aktuelleFSAnalysenNutz.keySet()) {
				aktuelleFSAnalysen.put(obj, null);
			}
			aktuelleFSAnalysenNutz.keySet().clear();
		}

		return ergebnis;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.bsvrz.dav.daf.main.ClientReceiverInterface#update(de.bsvrz.dav.daf.main.ResultData[])
	 */
	@Override
	public void update(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if (resultat != null) {
					final ResultData ergebnis = trigger(resultat);

					if (ergebnis != null) {
						if (ergebnis.getData() != null) {
							mqAnalyse.sendeDaten(ergebnis);
							letztesErgebnis = ergebnis;
						} else {
							if ((letztesErgebnis != null) && (letztesErgebnis.getData() != null)) {
								mqAnalyse.sendeDaten(ergebnis);
								letztesErgebnis = ergebnis;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * ************************************************************************* *
	 * Berechnungs-Methoden * *
	 * ************************************************************************.
	 *
	 * @param analyseDatum
	 *            the analyse datum
	 * @param attName
	 *            the att name
	 */

	/**
	 * Berechnet die Verkehrsstärken analog SE-02.00.00.00.00-AFo-4.0 S.118f.
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	private void berechneVerkehrsStaerke(final Data analyseDatum, final String attName) {
		final MesswertUnskaliert qAnalyse = new MesswertUnskaliert("Q" + attName); //$NON-NLS-1$
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		final boolean gueteBerechenbar = true;

		long summe = 0;
		final ArrayList<GWert> gueteWerte = new ArrayList<GWert>();
		/**
		 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar gekennzeichnet,
		 * so geht sie nicht in die jeweilige Berechnung des Zielwerts ein. Sind alle der bei der
		 * Berechnung beteiligten Größen als nicht ermittelbar gekennzeichnet, so wird der Zielwert
		 * mit den Statusflags nicht ermittelbar gekennzeichnet.
		 */
		int istNichtVorhanden = 0;
		for (final ResultData fsDaten : aktuelleFSAnalysenNutz.values()) {
			final MesswertUnskaliert fsWert = new MesswertUnskaliert(
					"q" + attName, fsDaten.getData()); //$NON-NLS-1$

			if (fsWert.isNichtErfasst()) {
				qAnalyse.setNichtErfasst(true);
			}
			if (fsWert.isFehlerhaftBzwImplausibel()) {
				nichtErmittelbarFehlerhaft = true;
				break;
			} else {
				if (fsWert.getWertUnskaliert() >= 0) {
					summe += fsWert.getWertUnskaliert();
					interpoliert |= fsWert.isInterpoliert();
					gueteWerte.add(new GWert(fsDaten.getData(), "q" + attName)); //$NON-NLS-1$
				} else {
					istNichtVorhanden++;
				}
			}
		}

		if (nichtErmittelbarFehlerhaft) {
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			if (aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				if (DUAUtensilien.isWertInWerteBereich(
						analyseDatum.getItem("Q" + attName).getItem("Wert"), summe)) { //$NON-NLS-1$//$NON-NLS-2$
					qAnalyse.setWertUnskaliert(summe);
					if (interpoliert) {
						qAnalyse.setInterpoliert(true);
					}
					if (gueteBerechenbar) {
						try {
							final GWert gesamtGuete = GueteVerfahren.summe(gueteWerte
									.toArray(new GWert[0]));
							qAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
							qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
						} catch (final GueteException e) {
							LOGGER.error("Guete-Index fuer Q" + attName + //$NON-NLS-1$
									" nicht berechenbar in " + analyseDatum, e);
							e.printStackTrace();
						}
					}
				} else {
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}
		}

		qAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Verkehrsstï¿½rken analog SE-02.00.00.00.00-AFo-4.0 S.118f.
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 * @param praefixGross
	 *            Präfix des Attributwertes groß
	 * @param praefixKlein
	 *            Präfix des Attributwertes klein
	 */
	private void berechneMittlereGeschwindigkeiten(final Data analyseDatum, final String attName,
			final String praefixGross, final String praefixKlein) {
		final MesswertUnskaliert qAnalyse = new MesswertUnskaliert(praefixGross + attName);

		final MesswertUnskaliert Q = new MesswertUnskaliert("Q" + attName, analyseDatum); //$NON-NLS-1$

		if (Q.isFehlerhaftBzwImplausibel()) {
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else if (Q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		} else {
			boolean nichtErmittelbarFehlerhaft = false;
			boolean interpoliert = false;
			boolean gueteBerechenbar = true;
			long summeQV = 0;
			long summeQ = 0;
			final ArrayList<GWert> gueteProdukte = new ArrayList<GWert>();

			int istNichtVorhanden = 0;
			for (final ResultData fsDaten : aktuelleFSAnalysenNutz.values()) {
				final MesswertUnskaliert q = new MesswertUnskaliert(
						"q" + attName, fsDaten.getData()); //$NON-NLS-1$
				final MesswertUnskaliert v = new MesswertUnskaliert(praefixKlein + attName,
						fsDaten.getData());

				if (q.isFehlerhaftBzwImplausibel() || v.isFehlerhaftBzwImplausibel()) {
					nichtErmittelbarFehlerhaft = true;
					break;
				} else if ((q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
						|| (v.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
					istNichtVorhanden++;
				} else {
					interpoliert |= q.isInterpoliert() || v.isInterpoliert();
					summeQV += q.getWertUnskaliert() * v.getWertUnskaliert();
					summeQ += q.getWertUnskaliert();
					try {
						gueteProdukte.add(GueteVerfahren.produkt(new GWert(fsDaten.getData(),
								"q" + attName), //$NON-NLS-1$
								new GWert(fsDaten.getData(), praefixKlein + attName)));
					} catch (final GueteException e) {
						gueteBerechenbar = false;
						LOGGER.error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$
								" nicht berechenbar in " + analyseDatum, e);
						e.printStackTrace();
					}
				}
			}

			if (nichtErmittelbarFehlerhaft) {
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else {
				if (aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					final long ergebnis = Math.round((double) summeQV / (double) summeQ);
					if (DUAUtensilien.isWertInWerteBereich(
							analyseDatum.getItem(praefixGross + attName).getItem("Wert"), ergebnis)) { //$NON-NLS-1$
						qAnalyse.setWertUnskaliert(ergebnis);
						if (interpoliert) {
							qAnalyse.setInterpoliert(true);
						}
						if (gueteBerechenbar) {
							try {
								final GWert gesamtGuete = GueteVerfahren.quotient(
										GueteVerfahren.summe(gueteProdukte.toArray(new GWert[0])),
										new GWert(analyseDatum, "Q" + attName) //$NON-NLS-1$
										);

								qAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
								qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
							} catch (final GueteException e) {
								LOGGER.error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$
										" nicht berechenbar in " + analyseDatum, e);
								e.printStackTrace();
							}
						}
					} else {
						qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}
		}

		for (final ResultData fsDaten : aktuelleFSAnalysenNutz.values()) {
			final MesswertUnskaliert v = new MesswertUnskaliert(praefixKlein + attName,
					fsDaten.getData());
			if (v.isNichtErfasst()) {
				qAnalyse.setNichtErfasst(true);
				break;
			}
		}

		qAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet <code>B</code> und <code>BMax</code> analog SE-02.00.00.00.00-AFo-4.0 S.118f.
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	private void berechneBelegung(final Data analyseDatum) {
		final MesswertUnskaliert BAnalyse = new MesswertUnskaliert("B");
		final MesswertUnskaliert BMaxAnalyse = new MesswertUnskaliert("BMax");

		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		final boolean gueteBerechenbar = true;

		long BMax = DUAKonstanten.NICHT_ERMITTELBAR;
		GWert gueteBMax = null;
		double bSumme = 0;
		final ArrayList<GWert> gueteWerte = new ArrayList<GWert>();

		int istNichtVorhanden = 0;
		for (final ResultData fsDatum : aktuelleFSAnalysenNutz.values()) {
			final DaMesswertUnskaliert bFs = new DaMesswertUnskaliert("b", fsDatum.getData()); //$NON-NLS-1$

			if (bFs.isFehlerhaftBzwImplausibel()) {
				nichtErmittelbarFehlerhaft = true;
				break;
			} else if (bFs.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				istNichtVorhanden++;
			} else {
				bSumme += bFs.getWertUnskaliert();

				if (bFs.isPlausibilisiert()) {
					interpoliert = true;
				}
				GWert guete = null;
				guete = new GWert(fsDatum.getData(), "b"); //$NON-NLS-1$
				gueteWerte.add(guete);

				/**
				 * BMax ermitteln
				 */
				if (bFs.getWertUnskaliert() > BMax) {
					BMax = bFs.getWertUnskaliert();
					gueteBMax = guete;
				}
			}
		}

		if (nichtErmittelbarFehlerhaft) {
			BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			BMaxAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			if (aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
				BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				BMaxAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				/**
				 * B setzen
				 */
				final long bB = Math.round(bSumme / aktuelleFSAnalysenNutz.keySet().size());
				if (DUAUtensilien.isWertInWerteBereich(
						analyseDatum.getItem("B").getItem("Wert"), bB)) { //$NON-NLS-1$ //$NON-NLS-2$
					BAnalyse.setWertUnskaliert(bB);
					if (interpoliert) {
						BAnalyse.setInterpoliert(true);
					}
					if (gueteBerechenbar) {
						try {
							final GWert gesamtGuete = GueteVerfahren.summe(gueteWerte
									.toArray(new GWert[0]));
							BAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
							BAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
						} catch (final GueteException e) {
							LOGGER.error(
									"Guete-Index fuer B nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}
				} else {
					BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

				/**
				 * BMax setzen
				 */
				if (DUAUtensilien.isWertInWerteBereich(
						analyseDatum.getItem("BMax").getItem("Wert"), BMax)) { //$NON-NLS-1$ //$NON-NLS-2$
					BMaxAnalyse.setWertUnskaliert(BMax);
					if (interpoliert) {
						BMaxAnalyse.setInterpoliert(true);
					}
					if (gueteBMax != null) {
						BMaxAnalyse.getGueteIndex().setWert(gueteBMax.getIndexUnskaliert());
						BMaxAnalyse.setVerfahren(gueteBMax.getVerfahren().getCode());
					}
				} else {
					BMaxAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}
		}

		BAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
		BMaxAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet <code>SKfz</code> analog SE-02.00.00.00.00-AFo-4.0 S.119f.
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	private void berechneStandardabweichung(final Data analyseDatum) {
		final MesswertUnskaliert sKfzAnalyse = new MesswertUnskaliert("SKfz"); //$NON-NLS-1$

		final MesswertUnskaliert qKfzUnskaliert = new MesswertUnskaliert("QKfz", analyseDatum); //$NON-NLS-1$
		final GWert qKfzGueteWert = new GWert(analyseDatum, "QKfz"); //$NON-NLS-1$

		boolean interpoliert = false;
		boolean gueteBerechenbar = true;

		if (qKfzUnskaliert.isFehlerhaftBzwImplausibel()) {
			sKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else if (qKfzUnskaliert.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
			sKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		} else {
			if (qKfzUnskaliert.getWertUnskaliert() > 1) {
				double sKfzWertOhneWurzel = 0;

				final DaMesswertUnskaliert vKfzUnskaliert = new DaMesswertUnskaliert(
						"VKfz", analyseDatum); //$NON-NLS-1$
				if (vKfzUnskaliert.isFehlerhaftBzwImplausibel()) {
					sKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				} else if (vKfzUnskaliert.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					sKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					final GWert vKfzGueteWert = new GWert(analyseDatum, "VKfz"); //$NON-NLS-1$

					interpoliert = vKfzUnskaliert.isInterpoliert()
							|| qKfzUnskaliert.isInterpoliert();

					final double vKfzWertUnskaliert = vKfzUnskaliert.getWertUnskaliert();
					final List<GWert> summanden = new ArrayList<GWert>();

					int istNichtVorhanden = 0;
					for (final ResultData fsDatum : aktuelleFSAnalysenNutz.values()) {
						final DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert(
								"qKfz", fsDatum.getData()); //$NON-NLS-1$
						final DaMesswertUnskaliert sKfz = new DaMesswertUnskaliert(
								"sKfz", fsDatum.getData()); //$NON-NLS-1$
						final DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert(
								"vKfz", fsDatum.getData()); //$NON-NLS-1$

						if (qKfz.isFehlerhaftBzwImplausibel() || vKfz.isFehlerhaftBzwImplausibel()
								|| sKfz.isFehlerhaftBzwImplausibel()) {
							sKfzAnalyse
							.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;
						} else if ((qKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
								|| (vKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
								|| (sKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
							istNichtVorhanden++;
						} else {
							interpoliert |= qKfz.isPlausibilisiert() || sKfz.isPlausibilisiert()
									|| vKfz.isPlausibilisiert();

							final double qKfzWert = qKfz.getWertUnskaliert();
							final double sKfzWert = sKfz.getWertUnskaliert();
							final double vKfzWert = vKfz.getWertUnskaliert();

							/**
							 * Berechnung
							 */
							sKfzWertOhneWurzel += ((qKfzWert * Math.pow(sKfzWert, 2.0)) + (qKfzWert * Math
									.pow(vKfzWert - vKfzWertUnskaliert, 2.0)))
									/ (qKfzUnskaliert.getWertUnskaliert() - 1.0);

							/**
							 * Guete
							 */
							final GWert qKfzGuete = new GWert(fsDatum.getData(), "qKfz"); //$NON-NLS-1$
							final GWert sKfzGuete = new GWert(fsDatum.getData(), "sKfz"); //$NON-NLS-1$
							final GWert vKfzGuete = new GWert(fsDatum.getData(), "vKfz"); //$NON-NLS-1$

							try {
								summanden.add(GueteVerfahren.quotient(GueteVerfahren.summe(
										GueteVerfahren.produkt(qKfzGuete,
												GueteVerfahren.exp(sKfzGuete, 2.0)), GueteVerfahren
												.produkt(qKfzGuete, GueteVerfahren.exp(
														GueteVerfahren.differenz(vKfzGuete,
																vKfzGueteWert), 2.0))),
										qKfzGueteWert));
							} catch (final GueteException e) {
								gueteBerechenbar = false;
								LOGGER.error(
										"Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
								e.printStackTrace();
							}
						}
					}

					if (aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
						sKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else if (sKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
						if (sKfzWertOhneWurzel >= 0) {
							final long SKfz = Math.round(Math.sqrt(sKfzWertOhneWurzel));

							if (DUAUtensilien.isWertInWerteBereich(analyseDatum
									.getItem("SKfz").getItem("Wert"), SKfz)) { //$NON-NLS-1$ //$NON-NLS-2$
								sKfzAnalyse.setWertUnskaliert(SKfz);
								if (interpoliert) {
									sKfzAnalyse.setInterpoliert(true);
								}
								if (gueteBerechenbar) {
									try {
										final GWert gesamtGuete = GueteVerfahren.exp(GueteVerfahren
												.summe(summanden.toArray(new GWert[0])), 0.5);

										sKfzAnalyse.getGueteIndex().setWert(
												gesamtGuete.getIndexUnskaliert());
										sKfzAnalyse.setVerfahren(gesamtGuete.getVerfahren()
												.getCode());
									} catch (final GueteException e) {
										LOGGER.error(
												"Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
										e.printStackTrace();
									}
								}
							} else {
								sKfzAnalyse
								.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							}
						} else {
							sKfzAnalyse
							.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						}
					}
				}
			} else {
				sKfzAnalyse.setWertUnskaliert(0);
				sKfzAnalyse.setInterpoliert(qKfzUnskaliert.isInterpoliert());
			}
		}

		if (sKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
			sKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}

		sKfzAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet (<code>ALkw</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneLkwAnteil(final Data analyseDatum) {
		final MesswertUnskaliert aLkwAnalyse = new MesswertUnskaliert("ALkw"); //$NON-NLS-1$
		final MesswertUnskaliert qLkw = new MesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert qKfz = new MesswertUnskaliert("QKfz", analyseDatum); //$NON-NLS-1$

		if (qLkw.isFehlerhaftBzwImplausibel() || qKfz.isFehlerhaftBzwImplausibel()) {
			aLkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else if ((qLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
				|| (qKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
			aLkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		} else {
			GWert aLkwGuete = null;
			final long aLkwWert = Math.round(((double) qLkw.getWertUnskaliert() / (double) qKfz
					.getWertUnskaliert()) * 100.0);

			if (DUAUtensilien.isWertInWerteBereich(
					analyseDatum.getItem("ALkw").getItem("Wert"), aLkwWert)) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					aLkwGuete = GueteVerfahren.quotient(new GWert(analyseDatum, "QLkw"), //$NON-NLS-1$
							new GWert(analyseDatum, "QKfz") //$NON-NLS-1$
							);
				} catch (final GueteException e) {
					LOGGER.error("Guete-Index fuer ALkw nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
					e.printStackTrace();
				}

				aLkwAnalyse.setWertUnskaliert(aLkwWert);
				aLkwAnalyse.setInterpoliert(qLkw.isInterpoliert() || qKfz.isInterpoliert());
				if (aLkwGuete != null) {
					aLkwAnalyse.getGueteIndex().setWert(aLkwGuete.getIndexUnskaliert());
					aLkwAnalyse.setVerfahren(aLkwGuete.getVerfahren().getCode());
				}
			} else {
				aLkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}

		aLkwAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Verkehrsstärken (<code>Kxxx</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	protected final void berechneDichte(final Data analyseDatum, final String attName) {
		final MesswertUnskaliert kAnalyse = new MesswertUnskaliert("K" + attName); //$NON-NLS-1$
		final MesswertUnskaliert q = new MesswertUnskaliert("Q" + attName, analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert v = new MesswertUnskaliert("V" + attName, analyseDatum); //$NON-NLS-1$

		if (q.isFehlerhaftBzwImplausibel() || v.isFehlerhaftBzwImplausibel()) {
			kAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			if ((v.getWertUnskaliert() == 0)
					|| (v.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
				if (parameter.isInitialisiert() && (letztesErgebnis != null)
						&& (letztesErgebnis.getData() != null)) {
					long grenz = -1;
					long max = -1;

					if (attName.startsWith("K")) { // Kfz //$NON-NLS-1$
						grenz = parameter.getKKfzGrenz();
						max = parameter.getKKfzMax();
					} else if (attName.startsWith("L")) { // Lkw //$NON-NLS-1$
						grenz = parameter.getKLkwGrenz();
						max = parameter.getKLkwMax();
					} else { // Pkw
						grenz = parameter.getKPkwGrenz();
						max = parameter.getKPkwMax();
					}

					final MesswertUnskaliert KTMinus1 = new MesswertUnskaliert(
							"K" + attName, letztesErgebnis.getData()); //$NON-NLS-1$
					if (KTMinus1.isFehlerhaftBzwImplausibel()) {
						kAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					} else if (KTMinus1.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
						kAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else {
						if (KTMinus1.getWertUnskaliert() < grenz) {
							kAnalyse.setWertUnskaliert(0);
						} else {
							kAnalyse.setWertUnskaliert(max);
						}
					}
				} else {
					kAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

			} else {
				if (q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					kAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					long kWert = Math.round((double) q.getWertUnskaliert()
							/ (double) v.getWertUnskaliert());
					if (DUAUtensilien.isWertInWerteBereich(analyseDatum
							.getItem("K" + attName).getItem("Wert"), kWert)) { //$NON-NLS-1$//$NON-NLS-2$
						final boolean interpoliert = q.isInterpoliert() || v.isInterpoliert();
						GWert kGuete = null;

						try {
							kGuete = GueteVerfahren.quotient(
									new GWert(analyseDatum, "Q" + attName), //$NON-NLS-1$
									new GWert(analyseDatum, "V" + attName) //$NON-NLS-1$
									);
						} catch (final GueteException e) {
							LOGGER.error("Guete-Index fuer K" + attName + " nicht berechenbar", e); //$NON-NLS-1$ //$NON-NLS-2$
							e.printStackTrace();
						}

						long max = -1;

						if (attName.startsWith("K")) { // Kfz //$NON-NLS-1$
							max = parameter.getKKfzMax();
						} else if (attName.startsWith("L")) { // Lkw //$NON-NLS-1$
							max = parameter.getKLkwMax();
						} else { // Pkw
							max = parameter.getKPkwMax();
						}

						if (!DatenaufbereitungLVE.getInstance().isIgnoreDichteMax()) {
							if ((max > 0) && (kWert > max)) {
								kWert = max;
							}
						}

						kAnalyse.setWertUnskaliert(kWert);
						kAnalyse.setInterpoliert(interpoliert);
						if (kGuete != null) {
							kAnalyse.getGueteIndex().setWert(kGuete.getIndexUnskaliert());
							kAnalyse.setVerfahren(kGuete.getVerfahren().getCode());
						}
					} else {
						kAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}
		}

		kAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Bemessungsverkehrsstaerke (<code>QB</code>) analog SE-02.00.00.00.00-AFo-4.0
	 * S.120f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneBemessungsVerkehrsstaerke(final Data analyseDatum) {
		final MesswertUnskaliert qbAnalyse = new MesswertUnskaliert("QB"); //$NON-NLS-1$
		final MesswertUnskaliert vPkw = new MesswertUnskaliert("VPkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert vLkw = new MesswertUnskaliert("VLkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert qPkw = new MesswertUnskaliert("QPkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert qLkw = new MesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$

		if (vLkw.isFehlerhaftBzwImplausibel() || qLkw.isFehlerhaftBzwImplausibel()
				|| (vLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
				|| (qLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
			if ((vPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
					|| (qPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
				qbAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				long qbWert = DUAKonstanten.NICHT_ERMITTELBAR;
				GWert qbGuete = GueteVerfahren.STD_FEHLERHAFT_BZW_NICHT_ERMITTELBAR;

				qbWert = qPkw.getWertUnskaliert();
				if (DUAUtensilien.isWertInWerteBereich(
						analyseDatum.getItem("QB").getItem("Wert"), qbWert)) { //$NON-NLS-1$//$NON-NLS-2$
					qbGuete = new GWert(analyseDatum, "QPkw"); //$NON-NLS-1$

					qbAnalyse.setWertUnskaliert(qbWert);
					qbAnalyse.setInterpoliert(qPkw.isInterpoliert());
					qbAnalyse.getGueteIndex().setWert(qbGuete.getIndexUnskaliert());
					qbAnalyse.setVerfahren(qbGuete.getVerfahren().getCode());
				} else {
					qbAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}
		} else {
			if (vPkw.isFehlerhaftBzwImplausibel() || qPkw.isFehlerhaftBzwImplausibel()) {
				qbAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else if ((vPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
					|| (qPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
				qbAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				if (parameter.isInitialisiert()) {
					final double k1 = parameter.getFlk1();
					final double k2 = parameter.getFlk2();

					double fL;
					if (vPkw.getWertUnskaliert() <= vLkw.getWertUnskaliert()) {
						fL = k1;
					} else {
						fL = k1 + (k2 * (vPkw.getWertUnskaliert() - vLkw.getWertUnskaliert()));
					}

					long qbWert = DUAKonstanten.NICHT_ERMITTELBAR;
					GWert qbGuete = GueteVerfahren.STD_FEHLERHAFT_BZW_NICHT_ERMITTELBAR;

					qbWert = qPkw.getWertUnskaliert() + Math.round(fL * qLkw.getWertUnskaliert());
					if (DUAUtensilien.isWertInWerteBereich(
							analyseDatum.getItem("QB").getItem("Wert"), qbWert)) { //$NON-NLS-1$//$NON-NLS-2$
						final GWert qPkwGuete = new GWert(analyseDatum, "QPkw"); //$NON-NLS-1$
						final GWert qLkwGuete = new GWert(analyseDatum, "QLkw"); //$NON-NLS-1$

						try {
							qbGuete = GueteVerfahren.summe(qPkwGuete,
									GueteVerfahren.gewichte(qLkwGuete, fL));
						} catch (final GueteException e) {
							LOGGER.error(
									"Guete-Index fuer QB nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}

						qbAnalyse.setWertUnskaliert(qbWert);
						qbAnalyse.setInterpoliert(qPkw.isInterpoliert() || qLkw.isInterpoliert());
						if (qbGuete != null) {
							qbAnalyse.getGueteIndex().setWert(qbGuete.getIndexUnskaliert());
							qbAnalyse.setVerfahren(qbGuete.getVerfahren().getCode());
						}
					} else {
						qbAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}

				} else {
					qbAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}

		}

		qbAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Bemessungsdichte (<code>KB</code>) analog SE-02.00.00.00.00-AFo-4.0 S.120f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneBemessungsdichte(final Data analyseDatum) {
		final MesswertUnskaliert KBAnalyse = new MesswertUnskaliert("KB"); //$NON-NLS-1$
		final MesswertUnskaliert QB = new MesswertUnskaliert("QB", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert VKfz = new MesswertUnskaliert("VKfz", analyseDatum); //$NON-NLS-1$

		if (QB.isFehlerhaftBzwImplausibel() || VKfz.isFehlerhaftBzwImplausibel()) {
			KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			if ((VKfz.getWertUnskaliert() == 0)
					|| (VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {

				if (parameter.isInitialisiert() && (letztesErgebnis != null)
						&& (letztesErgebnis.getData() != null)) {

					final MesswertUnskaliert KBTMinus1 = new MesswertUnskaliert(
							"KB", letztesErgebnis.getData()); //$NON-NLS-1$
					if (KBTMinus1.getWertUnskaliert() >= 0) {
						if (KBTMinus1.getWertUnskaliert() >= parameter.getKBGrenz()) {
							KBAnalyse.setWertUnskaliert(parameter.getKBMax());
						} else {
							KBAnalyse.setWertUnskaliert(0);
						}
					} else {
						KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				} else {
					KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

			} else {
				// normal berechnen
				if (QB.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					final long kbWert = Math.round((double) QB.getWertUnskaliert()
							/ (double) VKfz.getWertUnskaliert());

					if (DUAUtensilien.isWertInWerteBereich(
							analyseDatum.getItem("KB").getItem("Wert"), kbWert)) { //$NON-NLS-1$//$NON-NLS-2$
						final boolean interpoliert = QB.isInterpoliert() || VKfz.isInterpoliert();
						GWert kbGuete = null;
						try {
							kbGuete = GueteVerfahren.quotient(new GWert(analyseDatum, "QB"), //$NON-NLS-1$
									new GWert(analyseDatum, "VKfz") //$NON-NLS-1$
									);
						} catch (final GueteException e) {
							LOGGER.error(
									"Guete-Index fuer KB nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}

						KBAnalyse.setWertUnskaliert(kbWert);
						KBAnalyse.setInterpoliert(interpoliert);
						if (kbGuete != null) {
							KBAnalyse.getGueteIndex().setWert(kbGuete.getIndexUnskaliert());
							KBAnalyse.setVerfahren(kbGuete.getVerfahren().getCode());
						}
					} else {
						KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}
		}

		KBAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die gewichtete Differenzgeschwindigkeit (<code>VDelta</code>) im Messquerschnitt
	 * analog SE-02.00.00.00.00-AFo-4.0 S.120f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	private void berechneVDifferenz(final Data analyseDatum) {
		final MesswertUnskaliert vDeltaAnalyse = new MesswertUnskaliert("VDelta"); //$NON-NLS-1$

		if (aktuelleFSAnalysen.size() <= 1) {
			final DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert("VKfz"); //$NON-NLS-1$
			if (vKfz.isFehlerhaftBzwImplausibel()) {
				vDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else if (vKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				vDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				/**
				 * kopiere Wert
				 */
				vDeltaAnalyse.setWertUnskaliert(vKfz.getWertUnskaliert());
				vDeltaAnalyse.setFormalMax(vKfz.isFormalMax());
				vDeltaAnalyse.setFormalMin(vKfz.isFormalMin());
				vDeltaAnalyse.setGueteIndex(vKfz.getGueteIndex());
				vDeltaAnalyse.setVerfahren(vKfz.getVerfahren());
				vDeltaAnalyse.setLogischMax(vKfz.isLogischMax());
				vDeltaAnalyse.setLogischMin(vKfz.isLogischMin());
				vDeltaAnalyse.setInterpoliert(vKfz.isInterpoliert());
				vDeltaAnalyse.setNichtErfasst(vKfz.isNichtErfasst());
			}
		} else {
			boolean interpoliert = false;
			boolean gueteBerechnen = true;

			long VDeltaWert = 0;
			final List<GWert> gueteSummanden = new ArrayList<GWert>();

			if ((aktuelleFSAnalysen.keySet().size() == aktuelleFSAnalysenNutz.keySet().size())
					&& parameter.isInitialisiert()) {
				final MessQuerschnitt mq = MessQuerschnitt.getInstanz(messQuerschnitt);
				if (mq != null) {

					int istNichtVorhanden = 0;
					for (int i = 0; i < (mq.getFahrStreifen().size() - 1); i++) {
						int w;
						if (parameter.getWichtung().length > i) {
							w = parameter.getWichtung()[i];
						} else {
							vDeltaAnalyse
							.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;
						}
						final ResultData fsResultI = aktuelleFSAnalysen.get(mq.getFahrStreifen()
								.get(i).getSystemObject());
						final ResultData fsResultIPlus1 = aktuelleFSAnalysen.get(mq
								.getFahrStreifen().get(i + 1).getSystemObject());
						final MesswertUnskaliert vKfzI = new MesswertUnskaliert(
								"vKfz", fsResultI.getData()); //$NON-NLS-1$
						final MesswertUnskaliert vKfzIPlus1 = new MesswertUnskaliert(
								"vKfz", fsResultIPlus1.getData()); //$NON-NLS-1$

						if (vKfzI.isFehlerhaftBzwImplausibel()
								|| vKfzIPlus1.isFehlerhaftBzwImplausibel()) {
							vDeltaAnalyse
							.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;
						} else if ((vKfzI.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
								|| (vKfzIPlus1.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
							istNichtVorhanden++;
						} else {
							interpoliert = vKfzI.isInterpoliert() || vKfzIPlus1.isInterpoliert();
							VDeltaWert += w
									* Math.abs(vKfzI.getWertUnskaliert()
											- vKfzIPlus1.getWertUnskaliert());

							try {
								gueteSummanden.add(GueteVerfahren.gewichte(GueteVerfahren
										.differenz(new GWert(fsResultI.getData(), "vKfz"), //$NON-NLS-1$
												new GWert(fsResultIPlus1.getData(), "vKfz")), w)); //$NON-NLS-1$
							} catch (final GueteException e) {
								gueteBerechnen = false;
								LOGGER.error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
								e.printStackTrace();
							}
						}
					}

					if (vDeltaAnalyse.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT) {
						if (mq.getFahrStreifen().size() == istNichtVorhanden) {
							vDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
						} else if (!DUAUtensilien.isWertInWerteBereich(
								analyseDatum.getItem("VDelta").getItem("Wert"), VDeltaWert)) { //$NON-NLS-1$//$NON-NLS-2$
							vDeltaAnalyse
							.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						} else {
							vDeltaAnalyse.setWertUnskaliert(VDeltaWert);
							vDeltaAnalyse.setInterpoliert(interpoliert);
							if (gueteBerechnen) {
								try {
									final GWert guete = GueteVerfahren.summe(gueteSummanden
											.toArray(new GWert[0]));
									vDeltaAnalyse.getGueteIndex().setWert(
											guete.getIndexUnskaliert());
									vDeltaAnalyse.setVerfahren(guete.getVerfahren().getCode());
								} catch (final GueteException e) {
									LOGGER.error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
									e.printStackTrace();
								}
							}
						}
					}
				} else {
					vDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			} else {
				vDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}

		vDeltaAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

}
