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
package de.bsvrz.dua.dalve.analyse;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.SystemObject;
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

import java.util.*;

/**
 * In diesem Objekt werden alle aktuellen Werte die zur Berechnung der
 * Analysewerte eines Messquerschnitts notwendig sind gespeichert. Jedes mit dem
 * MQ assoziierte Fahrstreifendatum muss durch dieses Objekt (Methode
 * <code>trigger(..)</code>) geleitet werden um ggf. auch eine neue Berechnung
 * von Analysewerten auszulösen.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class DaAnalyseMessQuerschnitt implements ClientReceiverInterface {

	private static final Debug _debug = Debug.getLogger();

	/**
	 * Verbindung zum Analysemodul.
	 */
	protected MqAnalyseModul mqAnalyse = null;

	/**
	 * der mit diesem Objekt assoziierte Messquerschnitt.
	 */
	protected SystemObject messQuerschnitt = null;

	/**
	 * letztes für diesen Messquerschnitt errechnetes (veröffentlichtes)
	 * Ergebnis.
	 */
	protected ResultData letztesErgebnis = null;

	/**
	 * Aktuelle Analyseparameter dieses MQs.
	 */
	protected AtgVerkehrsDatenKurzZeitAnalyseMq parameter = null;

	/**
	 * Mapt alle hier betrachteten Fahrstreifen auf das letzte von ihnen
	 * empfangene Analysedatum.
	 */
	private Map<SystemObject, ResultData> aktuelleFSAnalysen = new HashMap<SystemObject, ResultData>();

	/**
	 * Alle aktuellen Fahrstreifenanalysen mit Nutzdaten.
	 */
	private Map<SystemObject, ResultData> aktuelleFSAnalysenNutz = new HashMap<SystemObject, ResultData>();

	/**
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zurück.
	 * Nach dieser Initialisierung ist das Objekt auf alle Daten (seiner
	 * assoziierten Fahrstreifen) angemeldet und analysiert ggf. Daten
	 * 
	 * @param analyseModul
	 *            Verbindung zum Analysemodul
	 * @param messQuerschnitt1
	 *            der Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException
	 *             wenn die Konfigurationsdaten des MQs nicht vollständig
	 *             ausgelesen werden konnte
	 */
	public DaAnalyseMessQuerschnitt initialisiere(MqAnalyseModul analyseModul, SystemObject messQuerschnitt1)
			throws DUAInitialisierungsException {
		if (mqAnalyse == null) {
			mqAnalyse = analyseModul;
		}

		this.messQuerschnitt = messQuerschnitt1;

		if (MessQuerschnitt.getInstanz(messQuerschnitt1) != null) {
			for (FahrStreifen fs : MessQuerschnitt.getInstanz(messQuerschnitt1).getFahrStreifen()) {
				this.aktuelleFSAnalysen.put(fs.getSystemObject(), null);
			}
		} else {
			throw new DUAInitialisierungsException("MQ-Konfiguration von " + messQuerschnitt1 + //$NON-NLS-1$
					" konnte nicht vollstaendig ausgelesen werden"); //$NON-NLS-1$
		}

		if (this.aktuelleFSAnalysen.keySet().isEmpty()) {
			Debug.getLogger().warning("Der MQ " + this.messQuerschnitt + " hat keine Fahrstreifen"); //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}

		/**
		 * Anmeldung auf Parameter und alle Daten der assoziierten
		 * Messquerschnitte
		 */
		this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(mqAnalyse.getDav(), messQuerschnitt1);
		mqAnalyse.getDav().subscribeReceiver(this, this.aktuelleFSAnalysen.keySet(),
				new DataDescription(mqAnalyse.getDav().getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS),
						mqAnalyse.getDav().getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)),
				ReceiveOptions.normal(), ReceiverRole.receiver());

		return this;
	}

	/**
	 * Dieser Methode sollten alle aktuellen Daten für alle mit diesem
	 * Messquerschnitt assoziierten Fahrstreifen uebergeben werden. Ggf. wird
	 * dadurch dann eine Berechnung der Analysewerte dieses Messquerschnittes
	 * ausgeluest.
	 * 
	 * @param triggerDatum
	 *            ein KZ-Datum eines assoziierten Fahrstreifens
	 * @return ein Analysedatum fuer diesen Messquerschnitt, wenn das
	 *         <code>triggerDatum</code> eine Berechnung ausgeloest hat, oder
	 *         <code>null</code> sonst
	 */
	protected ResultData trigger(ResultData triggerDatum) {
		ResultData ergebnis = null;
		this.aktuelleFSAnalysen.put(triggerDatum.getObject(), triggerDatum);

		/**
		 * Ein Analysedatum fuer den Fahrstreifen soll dann berechnet werden,
		 * wenn fuer alle Fahrstreifen, welche Nutzdaten haben (aber mindestenes
		 * einer) ein Datum mit dem gleichen Zeitstempel gekommen ist.
		 */
		this.aktuelleFSAnalysenNutz.clear();
		boolean berechne = false;
		long zeitStempel = -1;
		for (SystemObject fs : this.aktuelleFSAnalysen.keySet()) {
			ResultData fsDatum = this.aktuelleFSAnalysen.get(fs);

			if (fsDatum != null) {
				if (fsDatum.getData() != null) {
					this.aktuelleFSAnalysenNutz.put(fsDatum.getObject(), fsDatum);
					if (zeitStempel == -1) {
						/**
						 * erstes Datum
						 */
						zeitStempel = fsDatum.getDataTime();
						berechne = true;
					} else {
						/**
						 * Fuer den Fall, dass die Zeitstempel der Daten nicht
						 * uebereinstimmen, wird keine Daten veroeffentlicht
						 */
						if (fsDatum.getDataTime() != zeitStempel) {
							ergebnis = new ResultData(this.messQuerschnitt, mqAnalyse.pubBeschreibung,
									System.currentTimeMillis(), null);
							berechne = false;
							break;
						}
					}
				} else {
					/**
					 * Wenn fuer mindestens einen Fahrstreifen keine Nutzdaten
					 * vorliegen, dann veroeffentliche <code>keine Daten</code>
					 * fuer den Messquerschnitt
					 */
					ergebnis = new ResultData(this.messQuerschnitt, mqAnalyse.pubBeschreibung,
							System.currentTimeMillis(), null);
					berechne = false;
					break;
				}
			} else {
				/**
				 * Wenn nicht fuer ALLE Fahrstreifen des Messquerschnittes ein
				 * Datensatz vorliegt, dann mache nichts
				 */
				berechne = false;
				break;
			}
		}

		if (berechne) {
			final long datenZeit = zeitStempel;
			Data analyseDatum = mqAnalyse.getDav().createData(mqAnalyse.pubBeschreibung.getAttributeGroup());

			if (this.erfassungsIntervallOK()) {

				/**
				 * Berechne Verkehrsstärken
				 */
				this.berechneVerkehrsStaerke(analyseDatum, "Kfz"); //$NON-NLS-1$
				this.berechneVerkehrsStaerke(analyseDatum, "Lkw"); //$NON-NLS-1$
				this.berechneVerkehrsStaerke(analyseDatum, "Pkw"); //$NON-NLS-1$

				/**
				 * Berechne mittlere Geschwindigkeiten
				 */
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Kfz", "V", "v"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Lkw", "V", "v"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Pkw", "V", "v"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				this.berechneMittlereGeschwindigkeiten(analyseDatum, "Kfz", "Vg", "vg"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

				/**
				 * Belegung B und BMax
				 */
				this.berechneBelegung(analyseDatum);

				/**
				 * Standardabweichung
				 */
				this.berechneStandardabweichung(analyseDatum);

				/**
				 * Berechne LKW-Anteil
				 */
				this.berechneLkwAnteil(analyseDatum);

				/**
				 * Berechne Fahrzeugdichten
				 */
				this.berechneDichte(analyseDatum, "Kfz"); //$NON-NLS-1$
				this.berechneDichte(analyseDatum, "Lkw"); //$NON-NLS-1$
				this.berechneDichte(analyseDatum, "Pkw"); //$NON-NLS-1$

				/**
				 * Bemessungsverkehrsstärke
				 */
				this.berechneBemessungsVerkehrsstaerke(analyseDatum);

				/**
				 * Bemessungsdichte
				 */
				this.berechneBemessungsdichte(analyseDatum);

				/**
				 * Berechne die gewichtete Differenzgeschwindigkeit im
				 * Messquerschnitt
				 */
				this.berechneVDifferenz(analyseDatum);
			} else {
				this.setzeNichtErmittelbarFehlerhaft(analyseDatum);
			}

			ergebnis = new ResultData(this.messQuerschnitt, mqAnalyse.pubBeschreibung, datenZeit, analyseDatum);

			/**
			 * Puffer wieder zuruecksetzen
			 */
			for (SystemObject obj : this.aktuelleFSAnalysenNutz.keySet()) {
				this.aktuelleFSAnalysen.put(obj, null);
			}
			this.aktuelleFSAnalysenNutz.keySet().clear();
		}

		return ergebnis;
	}

	/**
	 * Gibt <tt>true</tt> zurück, wenn die Erfassungsintervalle der Fahrstreifen
	 * übereinstimmen
	 * 
	 * @return <tt>true</tt>, wenn die Erfassungsintervalle der Fahrstreifen
	 *         übereinstimmen, sonst <tt>false</tt>
	 */
	private boolean erfassungsIntervallOK() {
		long oldT = -1;
		for (ResultData resultData : aktuelleFSAnalysenNutz.values()) {
			long t = resultData.getData().getTimeValue("T").getMillis();
			if (oldT == -1) {
				oldT = t;
			} else {
				if (oldT != t)
					return false;
			}
		}
		return true;
	}

	/**
	 * Setzt ein datum komplett auf Nicht Ermittelbar/Fehlerhaft.
	 * 
	 * @param analyseDatum
	 *            Datum
	 */
	private void setzeNichtErmittelbarFehlerhaft(final Data analyseDatum) {
		for (Data data : analyseDatum) {
			DaMesswertUnskaliert mw = new DaMesswertUnskaliert(data.getName());
			mw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			mw.kopiereInhaltNach(analyseDatum);
		}
	}

	public void update(ResultData[] resultate) {
		if (resultate != null) {
			for (ResultData resultat : resultate) {
				if (resultat != null) {
					ResultData ergebnis = trigger(resultat);
					if (ergebnis != null) {
						if (ergebnis.getData() != null
								|| this.letztesErgebnis != null && this.letztesErgebnis.getData() != null) {
							mqAnalyse.sendeDaten(ergebnis);
						}
						this.letztesErgebnis = ergebnis;
					}
				}
			}
		}
	}

	/***************************************************************************
	 * * Berechnungs-Methoden * *
	 **************************************************************************/

	/**
	 * Berechnet die Verkehrsstärken analog SE-02.00.00.00.00-AFo-4.0 S.118f.
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	private void berechneVerkehrsStaerke(Data analyseDatum, String attName) {
		MesswertUnskaliert qAnalyse = new MesswertUnskaliert("Q" + attName); //$NON-NLS-1$
		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		boolean gueteBerechenbar = true;

		long summe = 0;
		ArrayList<GWert> gueteWerte = new ArrayList<GWert>();
		/**
		 * Ist eine der bei der Berechnung beteiligten Größen als nicht
		 * ermittelbar gekennzeichnet, so geht sie nicht in die jeweilige
		 * Berechnung des Zielwerts ein. Sind alle der bei der Berechnung
		 * beteiligten Größen als nicht ermittelbar gekennzeichnet, so wird der
		 * Zielwert mit den Statusflags nicht ermittelbar gekennzeichnet.
		 */
		int istNichtVorhanden = 0;
		for (ResultData fsDaten : this.aktuelleFSAnalysenNutz.values()) {
			MesswertUnskaliert fsWert = new MesswertUnskaliert("q" + attName, fsDaten.getData()); //$NON-NLS-1$

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
				if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("Q" + attName).getItem("Wert"), summe)) { //$NON-NLS-1$//$NON-NLS-2$
					qAnalyse.setWertUnskaliert(summe);
					if (interpoliert) {
						qAnalyse.setInterpoliert(true);
					}
					if (gueteBerechenbar) {
						try {
							GWert gesamtGuete = GueteVerfahren.summe(gueteWerte.toArray(new GWert[0]));
							qAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
							qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
						} catch (GueteException e) {
							Debug.getLogger().error("Guete-Index fuer Q" + attName + //$NON-NLS-1$
									" nicht berechenbar in " + analyseDatum, e); // $NON-NLS-1$
							e.printStackTrace();
						}
					}
				} else {
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}
		}

		qAnalyse.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet die Verkehrsstärken analog SE-02.00.00.00.00-AFo-4.0 S.118f.
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
	private void berechneMittlereGeschwindigkeiten(Data analyseDatum, String attName, String praefixGross,
			String praefixKlein) {
		MesswertUnskaliert qAnalyse = new MesswertUnskaliert(praefixGross + attName);

		GWert sumQKfzGuete; // $NON-NLS-1$
		double sumQKfz = 0;
		final List<GWert> qKfzGueten = new ArrayList<>(this.aktuelleFSAnalysenNutz.size());

		try {
			boolean interpoliert = false;

			for (ResultData fsDatum : this.aktuelleFSAnalysenNutz.values()) {
				DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("q" + attName, fsDatum.getData()); //$NON-NLS-1$
				DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert("v" + attName, fsDatum.getData()); //$NON-NLS-1$

				if (qKfz.getWertUnskaliert() >= 0 && vKfz.getWertUnskaliert() >= 0) {
					sumQKfz += qKfz.getWertUnskaliert();
					qKfzGueten.add(new GWert(fsDatum.getData(), "q" + attName));
				} else if (qKfz.isFehlerhaftBzwImplausibel()) {
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					return;
				}

			}

			try {
				sumQKfzGuete = GueteVerfahren.summe(qKfzGueten.toArray(new GWert[qKfzGueten.size()]));
			} catch (GueteException e) {
				Debug.getLogger().error("Guete-Index fuer Nenner von " + praefixGross + attName
						+ " nicht berechenbar in " + analyseDatum, e);
				sumQKfzGuete = GWert.getNichtErmittelbareGuete(GueteVerfahren.getZustand(qAnalyse.getVerfahren()));
			}

			boolean nichtErmittelbarFehlerhaft = false;
			boolean gueteBerechenbar = true;
			long summe = 0;
			ArrayList<GWert> gueteProdukte = new ArrayList<GWert>();

			int istNichtVorhanden = 0;
			for (ResultData fsDaten : this.aktuelleFSAnalysenNutz.values()) {
				MesswertUnskaliert q = new MesswertUnskaliert("q" + attName, fsDaten.getData()); //$NON-NLS-1$
				MesswertUnskaliert v = new MesswertUnskaliert(praefixKlein + attName, fsDaten.getData());

				if (q.isFehlerhaftBzwImplausibel() || v.isFehlerhaftBzwImplausibel()) {
					nichtErmittelbarFehlerhaft = true;
					break;
				} else if (q.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR
						|| v.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					istNichtVorhanden++;
				} else {
					interpoliert |= q.isInterpoliert() || v.isInterpoliert();
					summe += q.getWertUnskaliert() * v.getWertUnskaliert();
				}
				try {
					gueteProdukte.add(GueteVerfahren.produkt(new GWert(fsDaten.getData(), "q" + attName), //$NON-NLS-1$
							new GWert(fsDaten.getData(), praefixKlein + attName)));
				} catch (GueteException e) {
					gueteBerechenbar = false;
					Debug.getLogger().error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$
							" nicht berechenbar in " + analyseDatum, e); // $NON-NLS-1$
					e.printStackTrace();
				}
			}

			if (nichtErmittelbarFehlerhaft) {
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else {
				if (this.aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					long ergebnis = Math.round((double) summe / (double) sumQKfz);

					if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(praefixGross + attName).getItem("Wert"), //$NON-NLS-1$
							ergebnis)) {
						qAnalyse.setWertUnskaliert(ergebnis);
						if (interpoliert) {
							qAnalyse.setInterpoliert(true);
						}
						if (gueteBerechenbar) {
							try {
								GWert gesamtGuete = GueteVerfahren.quotient(
										GueteVerfahren.summe(gueteProdukte.toArray(new GWert[0])), sumQKfzGuete // $NON-NLS-1$
								);

								qAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
								qAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
							} catch (GueteException e) {
								Debug.getLogger().error("Guete-Index fuer " + praefixGross + attName + //$NON-NLS-1$
										" nicht berechenbar in " + analyseDatum, e); // $NON-NLS-1$
								e.printStackTrace();
							}
						}
					} else {
						qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}

			for (ResultData fsDaten : this.aktuelleFSAnalysenNutz.values()) {
				MesswertUnskaliert v = new MesswertUnskaliert(praefixKlein + attName, fsDaten.getData());
				if (v.isNichtErfasst()) {
					qAnalyse.setNichtErfasst(true);
					break;
				}
			}

		} finally {
			qAnalyse.kopiereInhaltNach(analyseDatum);
		}
	}

	/**
	 * Berechnet <code>B</code> und <code>BMax</code> analog
	 * SE-02.00.00.00.00-AFo-4.0 S.118f.
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	private void berechneBelegung(Data analyseDatum) {
		MesswertUnskaliert BAnalyse = new MesswertUnskaliert("B");
		MesswertUnskaliert BMaxAnalyse = new MesswertUnskaliert("BMax");

		boolean nichtErmittelbarFehlerhaft = false;
		boolean interpoliert = false;
		boolean gueteBerechenbar = true;

		long BMax = DUAKonstanten.NICHT_ERMITTELBAR;
		GWert gueteBMax = null;
		double bSumme = 0;
		ArrayList<GWert> gueteWerte = new ArrayList<GWert>();

		int istNichtVorhanden = 0;
		int n = 0;
		for (ResultData fsDatum : this.aktuelleFSAnalysenNutz.values()) {
			DaMesswertUnskaliert bFs = new DaMesswertUnskaliert("b", fsDatum.getData()); //$NON-NLS-1$

			if (bFs.isFehlerhaftBzwImplausibel()) {
				nichtErmittelbarFehlerhaft = true;
				break;
			} else if (bFs.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				istNichtVorhanden++;
			} else {
				bSumme += bFs.getWertUnskaliert();
				n++;
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
			if (this.aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
				BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				BMaxAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				/**
				 * B setzen
				 */
				long bB = Math.round(bSumme / (double) n);
				if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("B").getItem("Wert"), bB)) { //$NON-NLS-1$ //$NON-NLS-2$
					BAnalyse.setWertUnskaliert(bB);
					if (interpoliert) {
						BAnalyse.setInterpoliert(true);
					}
					if (gueteBerechenbar) {
						try {
							GWert gesamtGuete = GueteVerfahren.summe(gueteWerte.toArray(new GWert[0]));
							BAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
							BAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
						} catch (GueteException e) {
							Debug.getLogger().error("Guete-Index fuer B nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}
				} else {
					BAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

				/**
				 * BMax setzen
				 */
				if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("BMax").getItem("Wert"), BMax)) { //$NON-NLS-1$ //$NON-NLS-2$
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

		BAnalyse.kopiereInhaltNach(analyseDatum);
		BMaxAnalyse.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet <code>SKfz</code> analog SE-02.00.00.00.00-AFo-4.0 S.119f.
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	private void berechneStandardabweichung(Data analyseDatum) {
		MesswertUnskaliert SKfzAnalyse = new MesswertUnskaliert("SKfz"); //$NON-NLS-1$

		GWert sumQKfzGuete; // $NON-NLS-1$
		double sumQKfz = 0;
		final List<GWert> qKfzGueten = new ArrayList<>(this.aktuelleFSAnalysenNutz.size());

		try {

			int present = 0;
			boolean interpoliert = false;

			for (ResultData fsDatum : this.aktuelleFSAnalysenNutz.values()) {
				DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("qKfz", fsDatum.getData()); //$NON-NLS-1$

				DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert("vKfz", fsDatum.getData()); //$NON-NLS-1$

				DaMesswertUnskaliert sKfz = new DaMesswertUnskaliert("sKfz", fsDatum.getData()); //$NON-NLS-1$

				long q = qKfz.getWertUnskaliert();
				long v = vKfz.getWertUnskaliert();
				long s = sKfz.getWertUnskaliert();
				qKfzGueten.add(new GWert(fsDatum.getData(), "qKfz"));
				if (q >= 0 && v >= 0 && s >= 0) {
					sumQKfz += q;
					present++;
				} else if (qKfz.isFehlerhaftBzwImplausibel() || vKfz.isFehlerhaftBzwImplausibel()
						|| sKfz.isFehlerhaftBzwImplausibel()) {
					SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					return;
				}

			}

			try {
				sumQKfzGuete = GueteVerfahren.summe(qKfzGueten.toArray(new GWert[qKfzGueten.size()]));
			} catch (GueteException e) {
				Debug.getLogger().error("Guete-Index fuer Nenner von SKfz nicht berechenbar in " + analyseDatum, e);
				sumQKfzGuete = GWert.getNichtErmittelbareGuete(GueteVerfahren.getZustand(SKfzAnalyse.getVerfahren()));
			}

			if (present == 0) {
				SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				return;
			} else if (sumQKfz > 1) {
				double sKfzWertOhneWurzel = 0;

				DaMesswertUnskaliert VKfz = new DaMesswertUnskaliert("VKfz", analyseDatum); //$NON-NLS-1$
				if (VKfz.isFehlerhaftBzwImplausibel()) {
					SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				} else if (VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
					SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				} else {
					GWert VKfzGuete = new GWert(analyseDatum, "VKfz"); //$NON-NLS-1$

					interpoliert |= VKfz.isInterpoliert();

					double VKfzWert = VKfz.getWertUnskaliert();
					List<GWert> summanden = new ArrayList<GWert>();

					int istNichtVorhanden = 0;
					boolean gueteBerechenbar = true;
					for (ResultData fsDatum : this.aktuelleFSAnalysenNutz.values()) {
						DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("qKfz", fsDatum.getData()); //$NON-NLS-1$
						DaMesswertUnskaliert sKfz = new DaMesswertUnskaliert("sKfz", fsDatum.getData()); //$NON-NLS-1$
						DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert("vKfz", fsDatum.getData()); //$NON-NLS-1$

						if (qKfz.isFehlerhaftBzwImplausibel() || vKfz.isFehlerhaftBzwImplausibel()
								|| sKfz.isFehlerhaftBzwImplausibel()) {
							SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;
						} else if (qKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR
								|| (vKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR
										&& qKfz.getWertUnskaliert() != 0)
								|| sKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
							istNichtVorhanden++;
						} else {
							interpoliert |= qKfz.isPlausibilisiert() || sKfz.isPlausibilisiert()
									|| vKfz.isPlausibilisiert();

							double qKfzWert = qKfz.getWertUnskaliert();
							double sKfzWert = sKfz.getWertUnskaliert();
							double vKfzWert = vKfz.getWertUnskaliert();

							/**
							 * Berechnung
							 */
							sKfzWertOhneWurzel += (qKfzWert * Math.pow(sKfzWert, 2.0)
									+ qKfzWert * Math.pow(vKfzWert - VKfzWert, 2.0)) / (sumQKfz - 1.0);
						}

						/**
						 * Guete
						 */
						GWert qKfzGuete = new GWert(fsDatum.getData(), "qKfz"); // $NON-NLS-1$
						GWert sKfzGuete = new GWert(fsDatum.getData(), "sKfz"); // $NON-NLS-1$
						GWert vKfzGuete = new GWert(fsDatum.getData(), "vKfz"); // $NON-NLS-1$

						try {
							summanden
									.add(GueteVerfahren
											.quotient(
													GueteVerfahren.summe(
															GueteVerfahren.produkt(qKfzGuete,
																	GueteVerfahren.exp(sKfzGuete, 2.0)),
															GueteVerfahren.produkt(qKfzGuete,
																	GueteVerfahren.exp(GueteVerfahren
																			.differenz(vKfzGuete, VKfzGuete), 2.0))),
													sumQKfzGuete));
						} catch (GueteException e) {
							gueteBerechenbar = false;
							Debug.getLogger().error("Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
							e.printStackTrace();
						}
					}

					if (this.aktuelleFSAnalysenNutz.size() == istNichtVorhanden) {
						SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else if (SKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
						if (sKfzWertOhneWurzel >= 0) {
							long SKfz = Math.round(Math.sqrt(sKfzWertOhneWurzel));

							if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("SKfz").getItem("Wert"), //$NON-NLS-1$ //$NON-NLS-2$
									SKfz)) {
								SKfzAnalyse.setWertUnskaliert(SKfz);
								if (interpoliert) {
									SKfzAnalyse.setInterpoliert(true);
								}
								if (gueteBerechenbar) {
									try {
										GWert gesamtGuete = GueteVerfahren
												.exp(GueteVerfahren.summe(summanden.toArray(new GWert[0])), 0.5);

										SKfzAnalyse.getGueteIndex().setWert(gesamtGuete.getIndexUnskaliert());
										SKfzAnalyse.setVerfahren(gesamtGuete.getVerfahren().getCode());
									} catch (GueteException e) {
										Debug.getLogger()
												.error("Guete-Index fuer SKfz nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
										e.printStackTrace();
									}
								}
							} else {
								SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							}
						} else {
							SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						}
					}
				}
			} else {
				SKfzAnalyse.setWertUnskaliert(0);
			}
			SKfzAnalyse.setInterpoliert(interpoliert);

			if (SKfzAnalyse.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT) {
				SKfzAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		} finally {
			SKfzAnalyse.kopiereInhaltNach(analyseDatum);
		}
	}

	/**
	 * Berechnet (<code>ALkw</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneLkwAnteil(Data analyseDatum) {
		MesswertUnskaliert ALkwAnalyse = new MesswertUnskaliert("ALkw"); //$NON-NLS-1$
		MesswertUnskaliert QLkw = new MesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert QKfz = new MesswertUnskaliert("QKfz", analyseDatum); //$NON-NLS-1$

		if (QLkw.isFehlerhaftBzwImplausibel() || QKfz.isFehlerhaftBzwImplausibel()) {
			ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else if (QLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR
				|| QKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
			ALkwAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		} else {
			GWert ALkwGuete = null;
			long ALkwWert = Math.round((double) QLkw.getWertUnskaliert() / (double) QKfz.getWertUnskaliert() * 100.0);

			if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("ALkw").getItem("Wert"), ALkwWert)) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					ALkwGuete = GueteVerfahren.quotient(new GWert(analyseDatum, "QLkw"), //$NON-NLS-1$
							new GWert(analyseDatum, "QKfz") //$NON-NLS-1$
					);
				} catch (GueteException e) {
					Debug.getLogger().error("Guete-Index fuer ALkw nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
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

		ALkwAnalyse.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet (<code>ALkw</code>) analog SE-02.00.00.00.00-AFo-4.0 S.119f
	 *
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param suffix
	 *            der Suffix zur Bestimmung des Attributwertes
	 */
	protected final void berechneDichteVirtuell(Data analyseDatum, final String suffix) {
		MesswertUnskaliert KAnalyse = new MesswertUnskaliert("K" + suffix); //$NON-NLS-1$
		MesswertUnskaliert Q = new MesswertUnskaliert("Q" + suffix, analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert V = new MesswertUnskaliert("V" + suffix, analyseDatum); //$NON-NLS-1$

		if (Q.isFehlerhaftBzwImplausibel() || V.isFehlerhaftBzwImplausibel()) {
			KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else if (V.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
			KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
		} else {
			GWert KGuete = null;
			long KWert = Math.round((double) Math.max(0, Q.getWertUnskaliert()) / (double) V.getWertUnskaliert());

			if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("K" + suffix).getItem("Wert"), KWert)) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					KGuete = GueteVerfahren.quotient(new GWert(analyseDatum, "Q" + suffix), //$NON-NLS-1$
							new GWert(analyseDatum, "V" + suffix) //$NON-NLS-1$
					);
				} catch (GueteException e) {
					Debug.getLogger().error("Guete-Index fuer ALkw nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
					e.printStackTrace();
				}

				KAnalyse.setWertUnskaliert(KWert);
				KAnalyse.setInterpoliert(Q.isInterpoliert() || V.isInterpoliert());
				if (KGuete != null) {
					KAnalyse.getGueteIndex().setWert(KGuete.getIndexUnskaliert());
					KAnalyse.setVerfahren(KGuete.getVerfahren().getCode());
				}
			} else {
				KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}

		KAnalyse.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet die Verkehrsstärken (<code>Kxxx</code>) analog
	 * SE-02.00.00.00.00-AFo-4.0 S.119f
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 * @param attName
	 *            der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	protected final void berechneDichte(Data analyseDatum, final String attName) {
		MesswertUnskaliert KAnalyse = new MesswertUnskaliert("K" + attName); //$NON-NLS-1$

		GWert sumQKfzGuete; // $NON-NLS-1$
		GWert sumQKfzVKfzGuete; // $NON-NLS-1$
		double sumQKfz = 0;
		double sumQKfzVKfz = 0;
		final List<GWert> qKfzGueten = new ArrayList<>(this.aktuelleFSAnalysenNutz.size());
		final List<GWert> qKfzVKfzGueten = new ArrayList<>(this.aktuelleFSAnalysenNutz.size());

		// int numValues = 0;

		try {
			boolean interpoliert = false;

			for (ResultData fsDatum : this.aktuelleFSAnalysenNutz.values()) {
				DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("q" + attName, fsDatum.getData()); //$NON-NLS-1$
				DaMesswertUnskaliert vKfz = new DaMesswertUnskaliert("v" + attName, fsDatum.getData()); //$NON-NLS-1$

				if (qKfz.getWertUnskaliert() >= 0 && vKfz.getWertUnskaliert() >= 0) {
					sumQKfz += qKfz.getWertUnskaliert();
					sumQKfzVKfz += qKfz.getWertUnskaliert() * vKfz.getWertUnskaliert();
				} else if (qKfz.isFehlerhaftBzwImplausibel() || vKfz.isFehlerhaftBzwImplausibel()) {
					KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					return;
				}
				qKfzGueten.add(new GWert(fsDatum.getData(), "q" + attName));
				interpoliert |= qKfz.isInterpoliert();
				interpoliert |= vKfz.isInterpoliert();
				try {
					qKfzVKfzGueten.add(GueteVerfahren.produkt(new GWert(fsDatum.getData(), "q" + attName),
							new GWert(fsDatum.getData(), "v" + attName)));
				} catch (GueteException e) {
					_debug.error("Guete-Index fuer Nenner von K" + attName + " nicht berechenbar in " + analyseDatum,
							e);
					qKfzVKfzGueten
							.add(GWert.getNichtErmittelbareGuete(GueteVerfahren.getZustand(KAnalyse.getVerfahren())));
				}
			}

			try {
				sumQKfzGuete = GueteVerfahren
						.exp(GueteVerfahren.summe(qKfzGueten.toArray(new GWert[qKfzGueten.size()])), 2);
			} catch (GueteException e) {
				Debug.getLogger()
						.error("Guete-Index fuer Zähler von K" + attName + " nicht berechenbar in " + analyseDatum, e);
				sumQKfzGuete = GWert.getNichtErmittelbareGuete(GueteVerfahren.getZustand(KAnalyse.getVerfahren()));
			}
			try {
				sumQKfzVKfzGuete = GueteVerfahren.summe(qKfzVKfzGueten.toArray(new GWert[qKfzVKfzGueten.size()]));
			} catch (GueteException e) {
				Debug.getLogger()
						.error("Guete-Index fuer Zähler von K" + attName + " nicht berechenbar in " + analyseDatum, e);
				sumQKfzVKfzGuete = GWert.getNichtErmittelbareGuete(GueteVerfahren.getZustand(KAnalyse.getVerfahren()));
			}

			if (sumQKfzVKfz == 0) {
				if (this.parameter.isInitialisiert()) {
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

					ResultData erg = this.letztesErgebnis;
					Data data = erg == null ? null : erg.getData();
					if (data == null) {
						KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else {
						MesswertUnskaliert KTMinus1 = new MesswertUnskaliert("K" + attName, data); //$NON-NLS-1$
						if (KTMinus1.isFehlerhaftBzwImplausibel()) {
							KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						} else if (KTMinus1.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
							KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
						} else {
							if (KTMinus1.getWertUnskaliert() < grenz) {
								KAnalyse.setWertUnskaliert(0);
							} else {
								KAnalyse.setWertUnskaliert(Math.max(max, KTMinus1.getWertUnskaliert()));
							}
						}
					}
				} else {
					KAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}

			} else {
				long KWert = Math.round(sumQKfz * sumQKfz / sumQKfzVKfz);
				if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("K" + attName).getItem("Wert"), KWert)) { //$NON-NLS-1$//$NON-NLS-2$
					GWert KGuete = null;

					try {
						KGuete = GueteVerfahren.quotient(sumQKfzGuete, sumQKfzVKfzGuete);
					} catch (GueteException e) {
						Debug.getLogger().error("Guete-Index fuer K" + attName + " nicht berechenbar", e); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
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

		} finally {
			KAnalyse.kopiereInhaltNach(analyseDatum);
		}
	}

	/**
	 * Berechnet die Bemessungsverkehrsstaerke (<code>QB</code>) analog
	 * SE-02.00.00.00.00-AFo-4.0 S.120f
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneBemessungsVerkehrsstaerke(Data analyseDatum) {
		AtgVerkehrsDatenKurzZeitAnalyseMq parameter = this.parameter;
		MesswertUnskaliert qB = new MesswertUnskaliert("QB"); //$NON-NLS-1$
		boolean interpoliert = false;
		try {
			if (parameter.isInitialisiert()) {
				double k1 = parameter.getFlk1();
				double k2 = parameter.getFlk2();

				DaMesswertUnskaliert vPkw = new DaMesswertUnskaliert("VPkw", analyseDatum); //$NON-NLS-1$
				DaMesswertUnskaliert vLkw = new DaMesswertUnskaliert("VLkw", analyseDatum); //$NON-NLS-1$
				DaMesswertUnskaliert qPkw = new DaMesswertUnskaliert("QPkw", analyseDatum); //$NON-NLS-1$
				DaMesswertUnskaliert qLkw = new DaMesswertUnskaliert("QLkw", analyseDatum); //$NON-NLS-1$

				if (qLkw.isFehlerhaftBzwImplausibel() || vLkw.isFehlerhaftBzwImplausibel()
						|| qLkw.isFehlerhaftBzwImplausibel()) {
					qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				} else {
					double fL;
					GWert gueteqPkw = new GWert(analyseDatum, "QPkw");
					GWert gueteqLkw = new GWert(analyseDatum, "QLkw");

					if (vLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR || vLkw.getWertUnskaliert() == 0) {
						fL = k1;
					} else {

						if (vPkw.isFehlerhaftBzwImplausibel() || qPkw.isFehlerhaftBzwImplausibel()) {
							qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							return;
						} else if (vPkw.getWertUnskaliert() <= vLkw.getWertUnskaliert()) {
							fL = k1;
						} else {
							fL = k1 + k2 * (vPkw.getWertUnskaliert() - vLkw.getWertUnskaliert());
						}
					}

					long qPkwValue = qPkw.getWertUnskaliert();
					long qLkwValue = qLkw.getWertUnskaliert();

					if (qPkwValue == DUAKonstanten.NICHT_ERMITTELBAR && qLkwValue == DUAKonstanten.NICHT_ERMITTELBAR) {
						qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					} else {
						if (qPkwValue == DUAKonstanten.NICHT_ERMITTELBAR)
							qPkwValue = 0;
						if (qLkwValue == DUAKonstanten.NICHT_ERMITTELBAR)
							qLkwValue = 0;
						qB.setWertUnskaliert(Math.round(qPkwValue + fL * qLkwValue));
					}

					GWert gueteQB;
					try {
						gueteQB = GueteVerfahren.summe(gueteqPkw, gueteqLkw);
					} catch (GueteException e) {
						_debug.error("Guete von qB konnte nicht berechnet werden in " + analyseDatum, e);
						gueteQB = GWert.getNichtErmittelbareGuete(GueteVerfahren.getZustand(qB.getVerfahren()));
					}
					long indexUnskaliert = gueteQB.getIndexUnskaliert();
					qB.getGueteIndex().setWert(indexUnskaliert);
					qB.setVerfahren(gueteQB.getVerfahren().getCode());
					qB.setInterpoliert(qPkw.isPlausibilisiert() || qLkw.isPlausibilisiert() || interpoliert);
				}

			} else {
				qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		} finally {
			qB.kopiereInhaltNach(analyseDatum);
		}
	}

	/**
	 * Berechnet die Bemessungsdichte (<code>KB</code>) analog
	 * SE-02.00.00.00.00-AFo-4.0 S.120f
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	protected final void berechneBemessungsdichte(Data analyseDatum) {
		MesswertUnskaliert KBAnalyse = new MesswertUnskaliert("KB"); //$NON-NLS-1$
		MesswertUnskaliert QB = new MesswertUnskaliert("QB", analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert VKfz = new MesswertUnskaliert("VKfz", analyseDatum); //$NON-NLS-1$

		if (QB.isFehlerhaftBzwImplausibel() || VKfz.isFehlerhaftBzwImplausibel()) {
			KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			if (VKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				KBAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			} else if (VKfz.getWertUnskaliert() == 0) {

				if (this.parameter.isInitialisiert() && this.letztesErgebnis != null
						&& this.letztesErgebnis.getData() != null) {

					MesswertUnskaliert KBTMinus1 = new MesswertUnskaliert("KB", this.letztesErgebnis.getData()); //$NON-NLS-1$
					if (KBTMinus1.getWertUnskaliert() >= 0) {
						if (KBTMinus1.getWertUnskaliert() >= this.parameter.getKBGrenz()) {
							KBAnalyse.setWertUnskaliert(this.parameter.getKBMax());
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
					long KBWert = Math.round((double) QB.getWertUnskaliert() / (double) VKfz.getWertUnskaliert());

					if (DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("KB").getItem("Wert"), KBWert)) { //$NON-NLS-1$//$NON-NLS-2$
						boolean interpoliert = QB.isInterpoliert() || VKfz.isInterpoliert();
						GWert KBGuete = null;
						try {
							KBGuete = GueteVerfahren.quotient(new GWert(analyseDatum, "QB"), //$NON-NLS-1$
									new GWert(analyseDatum, "VKfz") //$NON-NLS-1$
							);
						} catch (GueteException e) {
							Debug.getLogger().error("Guete-Index fuer KB nicht berechenbar in " + analyseDatum, e); //$NON-NLS-1$
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

		KBAnalyse.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet die gewichtete Differenzgeschwindigkeit (<code>VDelta</code>)
	 * im Messquerschnitt analog SE-02.00.00.00.00-AFo-4.0 S.120f
	 * 
	 * @param analyseDatum
	 *            das Datum in das die Daten eingetragen werden sollen
	 */
	private final void berechneVDifferenz(Data analyseDatum) {
		MesswertUnskaliert VDeltaAnalyse = new MesswertUnskaliert("VDelta"); //$NON-NLS-1$

		if (this.aktuelleFSAnalysen.size() <= 1) {
			VDeltaAnalyse.setWertUnskaliert(0);
		} else {
			boolean interpoliert = false;
			boolean gueteBerechnen = true;

			long VDeltaWert = 0;
			long wSumme = 0;
			List<GWert> gueteSummanden = new ArrayList<GWert>();

			if (this.aktuelleFSAnalysen.keySet().size() == this.aktuelleFSAnalysenNutz.keySet().size()
					&& this.parameter.isInitialisiert()) {
				MessQuerschnitt mq = MessQuerschnitt.getInstanz(this.messQuerschnitt);

				List<FahrStreifen> fahrStreifen = new ArrayList<>(mq.getFahrStreifen());

				Collections.sort(fahrStreifen,
						(o1, o2) -> Integer.compare(o1.getLage().getCode(), o2.getLage().getCode()));

				if (mq != null) {

					int istNichtVorhanden = 0;
					int w = 0;
					for (int i = 0; i < fahrStreifen.size() - 1; i++) {
						if (this.parameter.getWichtung().length > i) {
							w = this.parameter.getWichtung()[i];
							wSumme += w;
						}
						ResultData fsResultI = this.aktuelleFSAnalysen.get(fahrStreifen.get(i).getSystemObject());
						ResultData fsResultIPlus1 = this.aktuelleFSAnalysen
								.get(fahrStreifen.get(i + 1).getSystemObject());
						MesswertUnskaliert vKfzI = new MesswertUnskaliert("vKfz", fsResultI.getData()); //$NON-NLS-1$
						MesswertUnskaliert vKfzIPlus1 = new MesswertUnskaliert("vKfz", fsResultIPlus1.getData()); //$NON-NLS-1$

						if (vKfzI.isFehlerhaftBzwImplausibel() || vKfzIPlus1.isFehlerhaftBzwImplausibel()) {
							VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							break;
						} else if (vKfzI.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR
								|| vKfzIPlus1.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
							istNichtVorhanden++;
						} else {
							interpoliert = vKfzI.isInterpoliert() || vKfzIPlus1.isInterpoliert();
							VDeltaWert += w * Math.abs(vKfzI.getWertUnskaliert() - vKfzIPlus1.getWertUnskaliert());

							try {
								gueteSummanden.add(GueteVerfahren
										.gewichte(GueteVerfahren.differenz(new GWert(fsResultI.getData(), "vKfz"), //$NON-NLS-1$
												new GWert(fsResultIPlus1.getData(), "vKfz")), (double) w)); //$NON-NLS-1$
							} catch (GueteException e) {
								gueteBerechnen = false;
								Debug.getLogger().error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
								e.printStackTrace();
							}
						}
					}

					VDeltaWert = Math.round(VDeltaWert / (double) wSumme);

					if (VDeltaAnalyse.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT) {
						if (istNichtVorhanden > 0) {
							VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
						} else if (!DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("VDelta").getItem("Wert"), //$NON-NLS-1$//$NON-NLS-2$
								VDeltaWert) || wSumme == 0) {
							VDeltaAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						} else {
							VDeltaAnalyse.setWertUnskaliert(VDeltaWert);
							VDeltaAnalyse.setInterpoliert(interpoliert);
							if (gueteBerechnen) {
								try {
									GWert guete = GueteVerfahren.summe(gueteSummanden.toArray(new GWert[0]));
									VDeltaAnalyse.getGueteIndex().setWert(guete.getIndexUnskaliert());
									VDeltaAnalyse.setVerfahren(guete.getVerfahren().getCode());
								} catch (GueteException e) {
									Debug.getLogger().error("Guete-Index fuer VDelta nicht berechenbar", e); //$NON-NLS-1$
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

		VDeltaAnalyse.kopiereInhaltNach(analyseDatum);
	}

}
