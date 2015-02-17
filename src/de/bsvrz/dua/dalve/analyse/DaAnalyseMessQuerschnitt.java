/**
 * Segment 4 Daten�bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Wei�enfelser Stra�e 67<br>
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
 * Berechnung von Analysewerten auszul�sen.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 * @version $Id$
 */
public class DaAnalyseMessQuerschnitt implements ClientReceiverInterface {

	/**
	 * Verbindung zum Analysemodul.
	 */
	protected static MqAnalyseModul mqAnalyse = null;

	/**
	 * der mit diesem Objekt assoziierte Messquerschnitt.
	 */
	protected SystemObject messQuerschnitt = null;

	/**
	 * letztes f�r diesen Messquerschnitt errechnetes (ver�ffentlichtes) Ergebnis.
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
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zur�ck. Nach dieser
	 * Initialisierung ist das Objekt auf alle Daten (seiner assoziierten Fahrstreifen) angemeldet
	 * und analysiert ggf. Daten
	 *
	 * @param analyseModul
	 *            Verbindung zum Analysemodul
	 * @param messQuerschnitt1
	 *            der Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException
	 *             wenn die Konfigurationsdaten des MQs nicht vollst�ndig ausgelesen werden konnte
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
			Debug.getLogger().warning("Der MQ " + messQuerschnitt + " hat keine Fahrstreifen"); //$NON-NLS-1$//$NON-NLS-2$
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
	 * Dieser Methode sollten alle aktuellen Daten f�r alle mit diesem Messquerschnitt assoziierten
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
		 * Ein Analysedatum fuer den Fahrstreifen soll dann berechnet werden, wenn f�r alle
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
				 * Berechne Verkehrsst�rken
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
				 * Bemessungsverkehrsst�rke
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
			 * Puffer wieder zur�cksetzen
			 */
			for (final SystemObject obj : aktuelleFSAnalysenNutz.keySet()) {
				aktuelleFSAnalysen.put(obj, null);
			}
			aktuelleFSAnalysenNutz.keySet().clear();
		}

		return ergebnis;
	}

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
	 * {@inheritDoc}
	 */
	@Override
	protected void finalize() throws Throwable {
		Debug.getLogger().warning("Der MQ " + messQuerschnitt + //$NON-NLS-1$
				" wird nicht mehr analysiert"); //$NON-NLS-1$
	}

	/***************************************************************************
	 * * Berechnungs-Methoden * *
	 **************************************************************************/

	/**
	 * Berechnet die Verkehrsst�rken analog SE-02.00.00.00.00-AFo-4.0 S.118f.
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
		 * Ist eine der bei der Berechnung beteiligten Gr��en als nicht ermittelbar gekennzeichnet,
		 * so geht sie nicht in die jeweilige Berechnung des Zielwerts ein. Sind alle der bei der
		 * Berechnung beteiligten Gr��en als nicht ermittelbar gekennzeichnet, so wird der Zielwert
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
							Debug.getLogger().error("Guete-Index fuer Q" + attName + //$NON-NLS-1$
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
	 * Berechnet die Verkehrsst�rken analog SE-02.00.00.00.00-AFo-4.0 S.118f.
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 * @param praefixGross
	 *            Pr�fix des Attributwertes gro�
	 * @param praefixKlein
	 *            Pr�fix des Attributwertes klein
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
						Debug.getLogger().error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$
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
								Debug.getLogger().error(
										"Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$
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
							Debug.getLogger().error(
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
		final MesswertUnskaliert SKfzAnalyse = new MesswertUnskaliert("SKfz"); //$NON-NLS-1$

		final MesswertUnskaliert QKfz = new MesswertUnskaliert("QKfz", analyseDatum); //$NON-NLS-1$
		final GWert QKfzGuete = new GWert(analyseDatum, "QKfz"); //$NON-NLS-1$

		boolean interpoliert = false;
		boolean gueteBerechenbar = true;

		if (QKfz.isFehlerhaftBzwImplausibel()) {
			SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else if (QKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
			SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		} else {
			if (QKfz.getWertUnskaliert() > 1) {
				double sKfzWertOhneWurzel = 0;

				final DaMesswertUnskaliert VKfz = new DaMesswertUnskaliert("VKfz", analyseDatum); //$NON-NLS-1$
				if (VKfz.isFehlerhaftBzwImplausibel()) {
					SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				} else if (VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					final GWert VKfzGuete = new GWert(analyseDatum, "VKfz"); //$NON-NLS-1$

					interpoliert = VKfz.isInterpoliert() || QKfz.isInterpoliert();

					final double VKfzWert = VKfz.getWertUnskaliert();
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
							SKfzAnalyse
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
									.pow(vKfzWert - VKfzWert, 2.0)))
									/ (QKfz.getWertUnskaliert() - 1.0);

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
																VKfzGuete), 2.0))), QKfzGuete));
							} catch (final GueteException e) {
								gueteBerechenbar = false;
								Debug.getLogger()
								.error("Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
								e.printStackTrace();
							}
						}
					}

					if (aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
						SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else if (SKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
						if (sKfzWertOhneWurzel >= 0) {
							final long SKfz = Math.round(Math.sqrt(sKfzWertOhneWurzel));

							if (DUAUtensilien.isWertInWerteBereich(analyseDatum
									.getItem("SKfz").getItem("Wert"), SKfz)) { //$NON-NLS-1$ //$NON-NLS-2$
								SKfzAnalyse.setWertUnskaliert(SKfz);
								if (interpoliert) {
									SKfzAnalyse.setInterpoliert(true);
								}
								if (gueteBerechenbar) {
									try {
										final GWert gesamtGuete = GueteVerfahren.exp(GueteVerfahren
												.summe(summanden.toArray(new GWert[0])), 0.5);

										SKfzAnalyse.getGueteIndex().setWert(
												gesamtGuete.getIndexUnskaliert());
										SKfzAnalyse.setVerfahren(gesamtGuete.getVerfahren()
												.getCode());
									} catch (final GueteException e) {
										Debug.getLogger()
										.error("Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
										e.printStackTrace();
									}
								}
							} else {
								SKfzAnalyse
								.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							}
						} else {
							SKfzAnalyse
							.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						}
					}
				}
			} else {
				SKfzAnalyse.setWertUnskaliert(0);
				SKfzAnalyse.setInterpoliert(QKfz.isInterpoliert());
			}
		}

		if (SKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
			SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}

		SKfzAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet (<code>ALkw</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneLkwAnteil(final Data analyseDatum) {
		final MesswertUnskaliert ALkwAnalyse = new MesswertUnskaliert("ALkw"); //$NON-NLS-1$
		final MesswertUnskaliert QLkw = new MesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert QKfz = new MesswertUnskaliert("QKfz", analyseDatum); //$NON-NLS-1$

		if (QLkw.isFehlerhaftBzwImplausibel() || QKfz.isFehlerhaftBzwImplausibel()) {
			ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else if ((QLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
				|| (QKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
			ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		} else {
			GWert ALkwGuete = null;
			final long ALkwWert = Math.round(((double) QLkw.getWertUnskaliert() / (double) QKfz
					.getWertUnskaliert()) * 100.0);

			if (DUAUtensilien.isWertInWerteBereich(
					analyseDatum.getItem("ALkw").getItem("Wert"), ALkwWert)) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					ALkwGuete = GueteVerfahren.quotient(new GWert(analyseDatum, "QLkw"), //$NON-NLS-1$
							new GWert(analyseDatum, "QKfz") //$NON-NLS-1$
							);
				} catch (final GueteException e) {
					Debug.getLogger().error(
							"Guete-Index fuer ALkw nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
					e.printStackTrace();
				}

				ALkwAnalyse.setWertUnskaliert(ALkwWert);
				ALkwAnalyse.setInterpoliert(QLkw.isInterpoliert() || QKfz.isInterpoliert());
				if (ALkwGuete != null) {
					ALkwAnalyse.getGueteIndex().setWert(ALkwGuete.getIndexUnskaliert());
					ALkwAnalyse.setVerfahren(ALkwGuete.getVerfahren().getCode());
				}
			} else {
				ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}

		ALkwAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Verkehrsst�rken (<code>Kxxx</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	protected final void berechneDichte(final Data analyseDatum, final String attName) {
		final MesswertUnskaliert KAnalyse = new MesswertUnskaliert("K" + attName); //$NON-NLS-1$
		final MesswertUnskaliert Q = new MesswertUnskaliert("Q" + attName, analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert V = new MesswertUnskaliert("V" + attName, analyseDatum); //$NON-NLS-1$

		if (Q.isFehlerhaftBzwImplausibel() || V.isFehlerhaftBzwImplausibel()) {
			KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			if ((V.getWertUnskaliert() == 0)
					|| (V.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
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
						KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					} else if (KTMinus1.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
						KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else {
						if (KTMinus1.getWertUnskaliert() < grenz) {
							KAnalyse.setWertUnskaliert(0);
						} else {
							KAnalyse.setWertUnskaliert(max);
						}
					}
				} else {
					KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

			} else {
				if (Q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					long KWert = Math.round((double) Q.getWertUnskaliert()
							/ (double) V.getWertUnskaliert());
					if (DUAUtensilien.isWertInWerteBereich(analyseDatum
							.getItem("K" + attName).getItem("Wert"), KWert)) { //$NON-NLS-1$//$NON-NLS-2$
						final boolean interpoliert = Q.isInterpoliert() || V.isInterpoliert();
						GWert KGuete = null;

						try {
							KGuete = GueteVerfahren.quotient(
									new GWert(analyseDatum, "Q" + attName), //$NON-NLS-1$
									new GWert(analyseDatum, "V" + attName) //$NON-NLS-1$
									);
						} catch (final GueteException e) {
							Debug.getLogger().error(
									"Guete-Index fuer K" + attName + " nicht berechenbar", e); //$NON-NLS-1$ //$NON-NLS-2$
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
							if ((max > 0) && (KWert > max)) {
								KWert = max;
							}
						}

						KAnalyse.setWertUnskaliert(KWert);
						KAnalyse.setInterpoliert(interpoliert);
						if (KGuete != null) {
							KAnalyse.getGueteIndex().setWert(KGuete.getIndexUnskaliert());
							KAnalyse.setVerfahren(KGuete.getVerfahren().getCode());
						}
					} else {
						KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}
		}

		KAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

	/**
	 * Berechnet die Bemessungsverkehrsstaerke (<code>QB</code>) analog SE-02.00.00.00.00-AFo-4.0
	 * S.120f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneBemessungsVerkehrsstaerke(final Data analyseDatum) {
		final MesswertUnskaliert QBAnalyse = new MesswertUnskaliert("QB"); //$NON-NLS-1$
		final MesswertUnskaliert VPkw = new MesswertUnskaliert("VPkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert VLkw = new MesswertUnskaliert("VLkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert QPkw = new MesswertUnskaliert("QPkw", analyseDatum); //$NON-NLS-1$
		final MesswertUnskaliert QLkw = new MesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$

		if (VLkw.isFehlerhaftBzwImplausibel() || QLkw.isFehlerhaftBzwImplausibel()
				|| (VLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
				|| (QLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
			if ((VPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
					|| (QPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
				QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				long QBWert = DUAKonstanten.NICHT_ERMITTELBAR;
				GWert QBGuete = GueteVerfahren.STD_FEHLERHAFT_BZW_NICHT_ERMITTELBAR;

				QBWert = QPkw.getWertUnskaliert();
				if (DUAUtensilien.isWertInWerteBereich(
						analyseDatum.getItem("QB").getItem("Wert"), QBWert)) { //$NON-NLS-1$//$NON-NLS-2$
					QBGuete = new GWert(analyseDatum, "QPkw"); //$NON-NLS-1$

					QBAnalyse.setWertUnskaliert(QBWert);
					QBAnalyse.setInterpoliert(QPkw.isInterpoliert());
					QBAnalyse.getGueteIndex().setWert(QBGuete.getIndexUnskaliert());
					QBAnalyse.setVerfahren(QBGuete.getVerfahren().getCode());
				} else {
					QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}
		} else {
			if (VPkw.isFehlerhaftBzwImplausibel() || QPkw.isFehlerhaftBzwImplausibel()) {
				QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else if ((VPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)
					|| (QPkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)) {
				QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				if (parameter.isInitialisiert()) {
					final double k1 = parameter.getFlk1();
					final double k2 = parameter.getFlk2();

					double fL;
					if (VPkw.getWertUnskaliert() <= VLkw.getWertUnskaliert()) {
						fL = k1;
					} else {
						fL = k1 + (k2 * (VPkw.getWertUnskaliert() - VLkw.getWertUnskaliert()));
					}

					long QBWert = DUAKonstanten.NICHT_ERMITTELBAR;
					GWert QBGuete = GueteVerfahren.STD_FEHLERHAFT_BZW_NICHT_ERMITTELBAR;

					QBWert = QPkw.getWertUnskaliert() + Math.round(fL * QLkw.getWertUnskaliert());
					if (DUAUtensilien.isWertInWerteBereich(
							analyseDatum.getItem("QB").getItem("Wert"), QBWert)) { //$NON-NLS-1$//$NON-NLS-2$
						final GWert QPkwGuete = new GWert(analyseDatum, "QPkw"); //$NON-NLS-1$
						final GWert QLkwGuete = new GWert(analyseDatum, "QLkw"); //$NON-NLS-1$

						try {
							QBGuete = GueteVerfahren.summe(QPkwGuete,
									GueteVerfahren.gewichte(QLkwGuete, fL));
						} catch (final GueteException e) {
							Debug.getLogger().error(
									"Guete-Index fuer QB nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}

						QBAnalyse.setWertUnskaliert(QBWert);
						QBAnalyse.setInterpoliert(QPkw.isInterpoliert() || QLkw.isInterpoliert());
						if (QBGuete != null) {
							QBAnalyse.getGueteIndex().setWert(QBGuete.getIndexUnskaliert());
							QBAnalyse.setVerfahren(QBGuete.getVerfahren().getCode());
						}
					} else {
						QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}

				} else {
					QBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}

		}

		QBAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
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
					final long KBWert = Math.round((double) QB.getWertUnskaliert()
							/ (double) VKfz.getWertUnskaliert());

					if (DUAUtensilien.isWertInWerteBereich(
							analyseDatum.getItem("KB").getItem("Wert"), KBWert)) { //$NON-NLS-1$//$NON-NLS-2$
						final boolean interpoliert = QB.isInterpoliert() || VKfz.isInterpoliert();
						GWert KBGuete = null;
						try {
							KBGuete = GueteVerfahren.quotient(new GWert(analyseDatum, "QB"), //$NON-NLS-1$
									new GWert(analyseDatum, "VKfz") //$NON-NLS-1$
									);
						} catch (final GueteException e) {
							Debug.getLogger().error(
									"Guete-Index fuer KB nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}

						KBAnalyse.setWertUnskaliert(KBWert);
						KBAnalyse.setInterpoliert(interpoliert);
						if (KBGuete != null) {
							KBAnalyse.getGueteIndex().setWert(KBGuete.getIndexUnskaliert());
							KBAnalyse.setVerfahren(KBGuete.getVerfahren().getCode());
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
	private final void berechneVDifferenz(final Data analyseDatum) {
		final MesswertUnskaliert VDeltaAnalyse = new MesswertUnskaliert("VDelta"); //$NON-NLS-1$

		if (aktuelleFSAnalysen.size() <= 1) {
			final DaMesswertUnskaliert VKfz = new DaMesswertUnskaliert("VKfz"); //$NON-NLS-1$
			if (VKfz.isFehlerhaftBzwImplausibel()) {
				VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else if (VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				/**
				 * kopiere Wert
				 */
				VDeltaAnalyse.setWertUnskaliert(VKfz.getWertUnskaliert());
				VDeltaAnalyse.setFormalMax(VKfz.isFormalMax());
				VDeltaAnalyse.setFormalMin(VKfz.isFormalMin());
				VDeltaAnalyse.setGueteIndex(VKfz.getGueteIndex());
				VDeltaAnalyse.setVerfahren(VKfz.getVerfahren());
				VDeltaAnalyse.setLogischMax(VKfz.isLogischMax());
				VDeltaAnalyse.setLogischMin(VKfz.isLogischMin());
				VDeltaAnalyse.setInterpoliert(VKfz.isInterpoliert());
				VDeltaAnalyse.setNichtErfasst(VKfz.isNichtErfasst());
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
							VDeltaAnalyse
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
							VDeltaAnalyse
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
								Debug.getLogger().error(
										"Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
								e.printStackTrace();
							}
						}
					}

					if (VDeltaAnalyse.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT) {
						if (mq.getFahrStreifen().size() == istNichtVorhanden) {
							VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
						} else if (!DUAUtensilien.isWertInWerteBereich(
								analyseDatum.getItem("VDelta").getItem("Wert"), VDeltaWert)) { //$NON-NLS-1$//$NON-NLS-2$
							VDeltaAnalyse
							.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						} else {
							VDeltaAnalyse.setWertUnskaliert(VDeltaWert);
							VDeltaAnalyse.setInterpoliert(interpoliert);
							if (gueteBerechnen) {
								try {
									final GWert guete = GueteVerfahren.summe(gueteSummanden
											.toArray(new GWert[0]));
									VDeltaAnalyse.getGueteIndex().setWert(
											guete.getIndexUnskaliert());
									VDeltaAnalyse.setVerfahren(guete.getVerfahren().getCode());
								} catch (final GueteException e) {
									Debug.getLogger().error(
											"Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
									e.printStackTrace();
								}
							}
						}
					}
				} else {
					VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			} else {
				VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}

		VDeltaAnalyse.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

}
