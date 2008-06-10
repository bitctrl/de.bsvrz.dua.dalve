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
package de.bsvrz.dua.dalve.analyse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnittVirtuell;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.daten.AtgMessQuerschnittVirtuellVLage;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * In diesem Objekt werden alle aktuellen Werte die zur Berechnung der
 * Analysewerte eines virtuellen Messquerschnitts notwendig sind gespeichert.
 * Wenn die Werte für ein bestimmtes Intervall bereit stehen (oder eine Timeout
 * abgelaufen ist), wird eine Berechnung durchgefuehrt und der Wert publiziert.
 * <br>
 * <b>Achtung: Verfahren auf Basis der Konfigurationsdaten aus Attributgruppe
 * <code>atg.messQuerschnittVirtuellVLage</code>.</b><br>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public class DaAnalyseMessQuerschnittVirtuellVLage extends
		DaAnalyseMessQuerschnitt {

	/**
	 * Mapt alle hier betrachteten Messquerschnitte auf das letzte von ihnen
	 * empfangene Analysedatum.
	 */
	private Map<SystemObject, ResultData> aktuelleMQAnalysen = new HashMap<SystemObject, ResultData>();

	/**
	 * Alle Anteile des VMQ.
	 */
	private Map<SystemObject, Double> mqAnteilsListe = new HashMap<SystemObject, Double>();

	/**
	 * der aufgeloesste virtuelle Messquerschnitt.
	 */
	private MessQuerschnittVirtuell mqv = null;

	/**
	 * MQ von dem die Geschwindigkeit uebernommen werden soll.
	 */
	private SystemObject geschwMQ = null;

	/**
	 * Tracker fuer die Erfassungsintervalldauer des MQ.
	 */
	private ErfassungsIntervallDauerMQ mqT = null;

	/**
	 * Initialisiert dieses Objekt und gibt die initialisierte Instanz zurueck.
	 * Nach dieser Initialisierung ist das Objekt auf alle Daten (seiner
	 * assoziierten Messquerschnitte) angemeldet und analysiert ggf. Daten
	 * 
	 * @param analyseModul
	 *            Verbindung zum Analysemodul (zum Publizieren)
	 * @param messQuerschnittVirtuell
	 *            der virtuelle Messquerschnitt
	 * @return die initialisierte Instanz dieses Objekts
	 * @throws DUAInitialisierungsException
	 *             wenn die Konfigurationsdaten des virtuellen MQs nicht
	 *             vollstaendig ausgelesen werden konnte
	 */
	public DaAnalyseMessQuerschnittVirtuellVLage initialisiere(
			MqAnalyseModul analyseModul, SystemObject messQuerschnittVirtuell)
			throws DUAInitialisierungsException {
		if (mqAnalyse == null) {
			mqAnalyse = analyseModul;
		}

		this.mqT = ErfassungsIntervallDauerMQ.getInstanz(mqAnalyse.getDav(),
				messQuerschnittVirtuell);

		if (this.mqT == null) {
			/**
			 * TODO: RuntimeException wieder rein: throw new
			 * RuntimeException("Erfassungsintervalldauer von VMQ " +
			 * messQuerschnittVirtuell + " kann nicht ermittelt werden.");
			 */
			Debug.getLogger().warning(
					"Erfassungsintervalldauer von VMQ "
							+ messQuerschnittVirtuell
							+ " kann nicht ermittelt werden.");
			return null;
		}

		this.messQuerschnitt = messQuerschnittVirtuell;
		this.mqv = MessQuerschnittVirtuell.getInstanz(messQuerschnitt);

		if (this.mqv.getAtgMessQuerschnittVirtuellVLage() == null) {
			throw new RuntimeException("Keine MQ-Bestandteile an VMQ "
					+ this.messQuerschnitt + " angegeben.");
		}

		if (this.mqv.getAtgMessQuerschnittVirtuellVLage()
				.getMessQuerSchnittBestandTeile().length == 0) {
			Debug.getLogger().warning(
					"Am virtuellen MQ " + this.messQuerschnitt
							+ " sind keine MQ referenziert.");
			return null;
		} else {
			for (AtgMessQuerschnittVirtuellVLage.AtlMessQuerSchnittBestandTeil bestandteil : this.mqv
					.getAtgMessQuerschnittVirtuellVLage()
					.getMessQuerSchnittBestandTeile()) {
				this.aktuelleMQAnalysen.put(bestandteil.getMQReferenz(), null);
				this.mqAnteilsListe.put(bestandteil.getMQReferenz(),
						bestandteil.getAnteil());
			}

			if (this.mqv.getAtgMessQuerschnittVirtuellVLage()
					.getMessQuerschnittGeschwindigkeit() != null) {
				this.geschwMQ = this.mqv.getAtgMessQuerschnittVirtuellVLage()
						.getMessQuerschnittGeschwindigkeit();
				if (!this.mqAnteilsListe.keySet().contains(this.geschwMQ)) {
					this.aktuelleMQAnalysen.put(this.geschwMQ, null);
				}
			}
		}

		this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(mqAnalyse
				.getDav(), messQuerschnitt);

		mqAnalyse.getDav().subscribeReceiver(
				this,
				this.aktuelleMQAnalysen.keySet(),
				new DataDescription(mqAnalyse.getDav().getDataModel()
						.getAttributeGroup("atg.verkehrsDatenKurzZeitMq"),
						mqAnalyse.getDav().getDataModel().getAspect(
								"asp.analyse"), (short) 0),
				ReceiveOptions.normal(), ReceiverRole.receiver());

		return this;
	}

	/**
	 * Dieser Methode sollten alle aktuellen Daten fuer alle mit diesem
	 * virtuellen Messquerschnitt assoziierten MQ uebergeben werden. Ggf. wird
	 * dadurch dann eine Berechnung der Analysewerte dieses Messquerschnittes
	 * ausgeloest.
	 * 
	 * @param triggerDatum
	 *            ein Analyse-Datum eines assoziierten Messquerschnitts
	 * @return ein Analysedatum fuer diesen virtuellen Messquerschnitt, wenn das
	 *         <code>triggerDatum</code> eine Berechnung ausgeloest hat, oder
	 *         <code>null</code> sonst
	 */
	@Override
	public ResultData trigger(ResultData triggerDatum) {
		ResultData ergebnis = null;
		this.aktuelleMQAnalysen.put(triggerDatum.getObject(), triggerDatum);

		if (this.isKeineDaten()) {
			ergebnis = new ResultData(this.messQuerschnitt,
					MqAnalyseModul.pubBeschreibung, triggerDatum.getDataTime(),
					null);
		} else {
			if (triggerDatum.getData() != null) {
				if (this.isAlleDatenVollstaendig()) {
					ergebnis = this.getErgebnisAufBasisAktuellerDaten();
				}
			}
		}
		
		System.out.println(ergebnis);
		return ergebnis;
	}

	/**
	 * Ermittelt, ob dieser virtuelle Messquerschnitt zur Zeit auf
	 * <code>keine Daten</code> stehen sollte.<br>
	 * Dies ist dann der Fall, wenn fuer mindestens einen assoziierten
	 * Messquerschnitt keine Nutzdaten zur Verfuegung stehen.
	 * 
	 * @return ob dieser virtuelle Messquerschnitt zur Zeit auf
	 *         <code>keine Daten</code> stehen sollte
	 */
	private boolean isKeineDaten() {
		for (SystemObject mq : this.aktuelleMQAnalysen.keySet()) {
			ResultData aktuellesMQDatum = this.aktuelleMQAnalysen.get(mq);
			if (aktuellesMQDatum == null || aktuellesMQDatum.getData() == null) {
				return true;
			}
		}

		return this.mqT.getT() == ErfassungsIntervallDauerMQ.NICHT_EINHEITLICH;
	}

	/**
	 * Erfragt, ob von den MQ, die an diesem virtuellen MQ erfasst sind, alle
	 * ein Datum mit Nutzdaten geliefert haben, dessen Zeitstempel spaeter als
	 * der des letzten hier errechneten Analysedatums ist.
	 * 
	 * @return ob alle Daten zur Berechnung eines neuen Intervalls da sind
	 */
	protected final boolean isAlleDatenVollstaendig() {
		boolean alleDatenVollstaendig = true;

		long letzteBerechnungsZeit = this.letztesErgebnis == null ? -1
				: this.letztesErgebnis.getDataTime();

		SortedSet<Long> letzteEingetroffeneZeitStempel = new TreeSet<Long>();

		for (SystemObject mq : this.aktuelleMQAnalysen.keySet()) {
			ResultData result = this.aktuelleMQAnalysen.get(mq);
			if (result == null || result.getData() == null) {
				alleDatenVollstaendig = false;
				break;
			} else {
				letzteEingetroffeneZeitStempel.add(result.getDataTime());
				if (result.getDataTime() <= letzteBerechnungsZeit
						|| letzteEingetroffeneZeitStempel.size() > 1) {
					alleDatenVollstaendig = false;
					break;
				}
			}
		}

		return alleDatenVollstaendig;
	}

	/**
	 * Diese Methode geht davon aus, dass keine weiteren Werte zur Berechnung
	 * des Analysedatums eintreffen werden und berechnet mit allen im Moment
	 * gepufferten Daten das Analysedatum.
	 * 
	 * @return ein Analysedatum
	 */
	private ResultData getErgebnisAufBasisAktuellerDaten() {
		ResultData ergebnis = null;

		Data analyseDatum = mqAnalyse.getDav().createData(
				MqAnalyseModul.pubBeschreibung.getAttributeGroup());

		/**
		 * Ermittle Werte fuer <code>VKfz, VLkw, VPkw</code> und
		 * <code>VgKfz</code> via Ersetzung
		 */
		ResultData ersetzung = this.aktuelleMQAnalysen.get(this.geschwMQ);

		for (String attName : new String[] { "VKfz", "VLkw", "VPkw", "VgKfz" }) {
			if (ersetzung != null && ersetzung.getData() != null) {
				new MesswertUnskaliert(attName, ersetzung.getData())
						.kopiereInhaltNachModifiziereIndex(analyseDatum);
			} else {
				Debug
						.getLogger()
						.error(
								"Es konnte kein Ersetzungsdatum fuer " + this.messQuerschnitt + //$NON-NLS-1$
										" im Attribut " + attName
										+ " ermittelt werden"); //$NON-NLS-1$ //$NON-NLS-2$					
				MesswertUnskaliert mw = new MesswertUnskaliert(attName);
				mw
						.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
			}
		}

		/**
		 * Setze Rest (<code>B, BMax, SKfz</code> und <code>VDelta</code>)
		 * auf <code>nicht ermittelbar/fehlerhaft</code>
		 */
		for (String attName : new String[] { "B", "BMax", "SKfz", "VDelta" }) {
			MesswertUnskaliert mw = new MesswertUnskaliert(attName);
			mw
					.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
		}

		/**
		 * Ermittle Werte für <code>QKfz, QLkw</code> und <code>QPkw</code>
		 */
		for (String attName : new String[] { "QKfz", "QLkw", "QPkw" }) {
			this.setBilanzDatum(analyseDatum, attName);
		}

		/**
		 * Berechne Werte für <code>ALkw, KKfz, KPkw, KLkw, QB</code> und
		 * <code>KB</code>
		 */
		this.berechneLkwAnteil(analyseDatum);
		this.berechneDichte(analyseDatum, "Kfz"); //$NON-NLS-1$
		this.berechneDichte(analyseDatum, "Lkw"); //$NON-NLS-1$
		this.berechneDichte(analyseDatum, "Pkw"); //$NON-NLS-1$
		this.berechneBemessungsVerkehrsstaerke(analyseDatum);
		this.berechneBemessungsdichte(analyseDatum);

		ResultData aktuellesReferenzDatum = this.getAktuellesReferenzDatum();
		if (aktuellesReferenzDatum != null
				&& aktuellesReferenzDatum.getData() != null) {
			ergebnis = new ResultData(this.messQuerschnitt,
					MqAnalyseModul.pubBeschreibung, aktuellesReferenzDatum
							.getDataTime(), analyseDatum);
		} else {
			/**
			 * Notbremse
			 */
			ergebnis = new ResultData(this.messQuerschnitt,
					MqAnalyseModul.pubBeschreibung, System.currentTimeMillis(),
					null);
		}

		return ergebnis;
	}

	/**
	 * Publiziert eine Analysedatum (so nicht <code>null</code> uebergeben
	 * wurde).
	 * 
	 * @param ergebnis
	 *            ein neu berechntes Analysedatum (oder <code>null</code>)
	 */
	private void publiziere(final ResultData ergebnis) {
		if (ergebnis != null) {

			System.out.println("Erg" + ergebnis);
			System.out.println(letztesErgebnis);
			if(letztesErgebnis != null){
				System.out.println("le:" + letztesErgebnis);
			}
			/**
			 * nur echt neue Daten versenden
			 */
			if (this.letztesErgebnis == null
					|| this.letztesErgebnis.getDataTime() < ergebnis
							.getDataTime()) {
				ResultData publikationsDatum = null;

				if (ergebnis.getData() == null) {
					/**
					 * Das folgende Flag zeigt an, ob dieser MQ zur Zeit auf
					 * "keine Daten" steht. Dies ist der Fall,<br>
					 * 1. wenn noch nie ein Datum für diesen MQ berechnet
					 * (versendet) wurde, oder<br>
					 * 2. wenn das letzte für diesen MQ berechnete (versendete)
					 * Datum keine Daten hatte.
					 */
					boolean aktuellKeineDaten = this.letztesErgebnis == null
							|| this.letztesErgebnis.getData() == null;

					if (!aktuellKeineDaten) {
						publikationsDatum = ergebnis;
					}
				} else {
					publikationsDatum = ergebnis;
				}

				if (publikationsDatum != null) {
					this.letztesErgebnis = ergebnis;
					mqAnalyse.sendeDaten(publikationsDatum);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(ResultData[] resultate) {
		if (resultate != null) {
			for (ResultData resultat : resultate) {
				if (resultat != null) {
					this.publiziere(this.trigger(resultat));
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finalize() throws Throwable {
		Debug.getLogger().warning("Der virtuelle MQ " + this.messQuerschnitt + //$NON-NLS-1$
				" wird nicht mehr analysiert"); //$NON-NLS-1$
	}

	/***************************************************************************
	 * * Berechnungs-Methoden * *
	 **************************************************************************/

	/**
	 * Erfragt das aktuelle Referenzdatum. Das ist das Datum, dessen Zeitstempel
	 * und Intervall mit dem des Analysedatums identisch ist, dass auf Basis der
	 * aktuellen Daten produziert werden kann
	 * 
	 * @return das aktuelle Referenzdatum, oder <code>null</code>
	 */
	private ResultData getAktuellesReferenzDatum() {
		ResultData ergebnis = null;

		for (ResultData aktuellesDatum : this.aktuelleMQAnalysen.values()) {
			if (aktuellesDatum != null && aktuellesDatum.getData() != null) {
				/**
				 * Dieses Datum wird zur Berechnung des Ausgangsdatums
				 * herangezogen
				 */
				ergebnis = aktuellesDatum;
			}
		}

		return ergebnis;
	}

	/**
	 * Setzt die Verkehrsstärke für diesen virtuellen Messquerschnitt in den
	 * Attributen <code>QKfz, QLkw</code> und <code>QPkw</code>.
	 * 
	 * @param analyseDatum
	 *            das zu modifizierende Datum.
	 * @param attName
	 *            der Name des Attributs, für das die Verkehrsstärke gesetzt
	 *            werden soll
	 */
	private void setBilanzDatum(Data analyseDatum, String attName) {
		List<QWert> qWerte = new ArrayList<QWert>();

		for (SystemObject mq : this.mqAnteilsListe.keySet()) {
			qWerte.add(new QWert(this.aktuelleMQAnalysen.get(mq), attName,
					this.mqAnteilsListe.get(mq)));
		}

		QWert qQ = null;
		if (!qWerte.isEmpty()) {
			qQ = QWert.summe(qWerte.toArray(new QWert[0]));
		}

		MesswertUnskaliert mw = new MesswertUnskaliert(attName);
		if (qQ == null || !qQ.isExportierbarNach(analyseDatum)) {
			mw
					.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		} else {
			mw = qQ.getWert();
		}
		mw.kopiereInhaltNachModifiziereIndex(analyseDatum);
	}

}
