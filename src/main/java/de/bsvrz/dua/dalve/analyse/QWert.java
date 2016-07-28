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

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Instanzen dieser Klasse repräsentieren Q-Werte die entsprechend
 * SE-02.00.00.00.00-AFo-4.0 (S.121) additiv und subtraktiv miteinander
 * verknüpft werden können.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class QWert {

	/**
	 * Der Wert.
	 */
	private MesswertUnskaliert wert = null;

	/**
	 * der Anteil, mit dem dieser Werte in die Berechnung eingeht.
	 */
	private double anteil = 1.0;

	/**
	 * Standardkonstruktor.
	 * 
	 * @param resultat
	 *            ein KZD-Resultat (kann auch <code>null</code> sein)
	 * @param attName
	 *            der Name des Zielattributes (Q-Wert) innerhalb des
	 *            KZD-Resultates
	 */
	public QWert(ResultData resultat, String attName) {
		if (resultat != null && resultat.getData() != null) {
			this.wert = new MesswertUnskaliert(attName, resultat.getData());
		}
	}

	/**
	 * Standardkonstruktor.
	 * 
	 * @param resultat
	 *            ein KZD-Resultat (kann auch <code>null</code> sein)
	 * @param attName
	 *            der Name des Zielattributes (Q-Wert) innerhalb des
	 *            KZD-Resultates
	 * @param derAnteil
	 *            der Anteil mit dem dieser Wert in die Berechnung eingehen soll
	 */
	public QWert(ResultData resultat, String attName, double derAnteil) {
		if (resultat != null && resultat.getData() != null) {
			this.wert = new MesswertUnskaliert(attName, resultat.getData());
			this.anteil = derAnteil;
		}
	}

	/**
	 * Interner Ergebniskonstruktor.
	 * 
	 * @param attName
	 *            der Name des Zielattributes (Q-Wert)
	 */
	private QWert(String attName) {
		this.wert = new MesswertUnskaliert(attName);
	}

	/**
	 * Erfragt, ob dieses Datum verrechenbar ist. Dies ist dann der Fall, wenn
	 * das Datum Nutzdaten enthält, die <code> &gt;= 0</code> sind
	 * 
	 * @return ob dieses Datum verrechenbar ist
	 */
	public final boolean isVerrechenbar() {
		return this.wert != null && this.wert.getWertUnskaliert() >= 0;
	}

	/**
	 * Erfragt, ob der in diesem Objekt stehende Wert in das übergebene Datum
	 * exportierbar ist.
	 * 
	 * @param datum
	 *            ein KZD-Nutzdatum
	 * @return ob der in diesem Objekt stehende Wert in das übergebene Datum
	 *         exportierbar ist
	 */
	public final boolean isExportierbarNach(Data datum) {
		boolean exportierbar = false;

		if (datum != null && this.wert != null) {
			exportierbar = DUAUtensilien.isWertInWerteBereich(
					datum.getItem(this.wert.getName()).getItem("Wert"), //$NON-NLS-1$
					this.wert.getWertUnskaliert());
		}

		return exportierbar;
	}

	/**
	 * Erfragt den aktuellen Zustand des Messwertes, der von diesem Objekt
	 * repräsentiert wird.
	 * 
	 * @return der aktuelle Zustand des Messwertes, der von diesem Objekt
	 *         repräsentiert wird
	 */
	public final MesswertUnskaliert getWert() {
		return this.wert;
	}

	/**
	 * Erfragt den Anteil, mit dem dieser Werte in die Berechnung eingeht.
	 * 
	 * @return der Anteil, mit dem dieser Werte in die Berechnung eingeht.
	 */
	public final double getAnteil() {
		return this.anteil;
	}

	/**
	 * Berechnet die Summe der beiden übergebenen Werte entsprechend
	 * SE-02.00.00.00.00-AFo-4.0 (S.121).
	 * 
	 * @param summanden
	 *            Liste der Summanden (<b>Muss mindestens ein Element
	 *            enthalten</b>).
	 * @return die Summe der beiden Summanden oder <code>null</code>, wenn diese
	 *         nicht ermittelt werden konnte
	 */
	public static final QWert summe(QWert... summanden) {
		QWert ergebnis = new QWert(summanden[0].getWert().getName());
		ergebnis.getWert().setWertUnskaliert(
				DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);

		double ergebnisSkaliertBisJetzt = 0;
		List<GWert> gueteListe = new ArrayList<GWert>();
		boolean interpoliert = false;

		for (QWert summand : summanden) {
			if (summand.getWert() == null
					|| summand.getWert().isFehlerhaftBzwImplausibel()) {
				return ergebnis;
			}
			if (summand.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				ergebnis.getWert().setWertUnskaliert(
						DUAKonstanten.NICHT_ERMITTELBAR);
				return ergebnis;
			}

			double sd = summand.getWert().getWertUnskaliert()
					* summand.getAnteil();
			ergebnisSkaliertBisJetzt += sd;

			interpoliert |= summand.getWert().isInterpoliert();

			try {
				gueteListe.add(GueteVerfahren.gewichte(
						new GWert(summand.getWert().getGueteIndex(),
								GueteVerfahren.getZustand(summand.getWert()
										.getVerfahren()), false), Math
								.abs(summand.getAnteil())));
			} catch (GueteException e) {
				e.printStackTrace();
				Debug.getLogger().error(
						"Guete konnte nicht gewichtet werden:\nGuete: "
								+ new GWert(summand.getWert().getGueteIndex(),
										GueteVerfahren.getZustand(summand
												.getWert().getVerfahren()),
										false) + "\nVorgesehenes Gewicht: "
								+ Math.abs(summand.getAnteil()));
			}

		}

		ergebnis.getWert().setWertUnskaliert(
				Math.round(ergebnisSkaliertBisJetzt));

		ergebnis.getWert().setInterpoliert(interpoliert);

		try {
			GWert gueteGesamt = GueteVerfahren.summe(gueteListe
					.toArray(new GWert[0]));
			ergebnis.getWert().getGueteIndex()
					.setWert(gueteGesamt.getIndexUnskaliert());
			ergebnis.getWert().setVerfahren(
					gueteGesamt.getVerfahren().getCode());
		} catch (GueteException e) {
			e.printStackTrace();
			String gsum = "";
			for (GWert guete : gueteListe) {
				gsum += guete + "\n";
			}
			Debug.getLogger().error(
					"Guete-Summe konnte nicht ermittelt werden.\n***Summanden***\n"
							+ gsum);
		}

		return ergebnis;
	}

	/**
	 * Berechnet die Summe der beiden übergebenen Werte entsprechend
	 * SE-02.00.00.00.00-AFo-4.0 (S.121).
	 * 
	 * @param summand1
	 *            1. Summand (kann auch <code>null</code> sein)
	 * @param summand2
	 *            2. Summand (kann auch <code>null</code> sein)
	 * @return die Summe der beiden Summanden oder <code>null</code>, wenn diese
	 *         nicht ermittelt werden konnte
	 */
	public static final QWert summe(QWert summand1, QWert summand2) {
		QWert ergebnis = null;

		if (summand1 != null && summand2 != null && summand1.getWert() != null
				&& summand2.getWert() != null) {
			if (summand1.getWert().isFehlerhaftBzwImplausibel()
					|| summand2.getWert().isFehlerhaftBzwImplausibel()) {
				ergebnis = new QWert(summand1.getWert().getName());
				ergebnis.getWert().setWertUnskaliert(
						DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else if (summand1.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR
					|| summand2.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				ergebnis = new QWert(summand1.getWert().getName());
				ergebnis.getWert().setWertUnskaliert(
						DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				ergebnis = new QWert(summand1.getWert().getName());

				double s1 = summand1.getWert().getWertUnskaliert()
						* summand1.getAnteil();
				double s2 = summand2.getWert().getWertUnskaliert()
						* summand2.getAnteil();
				double ergebnisSkaliert = s1 + s2;

				ergebnis.getWert().setWertUnskaliert(
						Math.round(ergebnisSkaliert));

				ergebnis.getWert().setInterpoliert(
						summand1.getWert().isInterpoliert()
								|| summand2.getWert().isInterpoliert());

				/**
				 * Gueteberechnung
				 */
				try {
					GWert gueteSummand1 = new GWert(summand1.getWert()
							.getGueteIndex(),
							GueteVerfahren.getZustand(summand1.getWert()
									.getVerfahren()), false);

					GWert gueteSummand2 = new GWert(summand2.getWert()
							.getGueteIndex(),
							GueteVerfahren.getZustand(summand2.getWert()
									.getVerfahren()), false);

					GWert gueteGesamt = GueteVerfahren.summe(
							GueteVerfahren.gewichte(gueteSummand1,
									Math.abs(summand1.getAnteil())),
							GueteVerfahren.gewichte(gueteSummand2,
									Math.abs(summand2.getAnteil())));
					ergebnis.getWert().getGueteIndex()
							.setWert(gueteGesamt.getIndexUnskaliert());
					ergebnis.getWert().setVerfahren(
							gueteGesamt.getVerfahren().getCode());
				} catch (GueteException e) {
					e.printStackTrace();
					Debug.getLogger()
							.error("Guete-Summe konnte nicht ermittelt werden.\n***Summand1***\n" + summand1.getWert() + //$NON-NLS-1$
									"\n***Summand2***\n" + summand2.getWert()); //$NON-NLS-1$
				}
			}
		}

		return ergebnis;
	}

	/**
	 * Berechnet die Differenz der beiden übergebenen Werte entsprechend
	 * SE-02.00.00.00.00-AFo-4.0 (S.121).
	 * 
	 * @param minuend
	 *            der Minuend
	 * @param subtrahend
	 *            der Subtrahend
	 * @return die Differenz der beiden übergebenen Werte entsprechend
	 *         SE-02.00.00.00.00-AFo-4.0 (S.121) oder <code>null</code>, wenn
	 *         diese nicht ermittelt werden konnte
	 */
	public static final QWert differenz(QWert minuend, QWert subtrahend) {
		QWert ergebnis = null;

		if (minuend != null && minuend.getWert() != null && subtrahend != null
				&& subtrahend.getWert() != null) {
			if (minuend.getWert().isFehlerhaftBzwImplausibel()
					|| subtrahend.getWert().isFehlerhaftBzwImplausibel()) {
				ergebnis = new QWert(minuend.getWert().getName());
				ergebnis.getWert().setWertUnskaliert(
						DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			} else if (minuend.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR
					|| subtrahend.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				ergebnis = new QWert(minuend.getWert().getName());
				ergebnis.getWert().setWertUnskaliert(
						DUAKonstanten.NICHT_ERMITTELBAR);
			} else {
				ergebnis = new QWert(minuend.getWert().getName());

				double s1 = minuend.getWert().getWertUnskaliert()
						* minuend.getAnteil();
				double s2 = subtrahend.getWert().getWertUnskaliert()
						* subtrahend.getAnteil();
				double ergebnisSkaliert = s1 - s2;

				ergebnis.getWert().setWertUnskaliert(
						Math.round(ergebnisSkaliert));

				ergebnis.getWert().setInterpoliert(
						minuend.getWert().isInterpoliert()
								|| subtrahend.getWert().isInterpoliert());

				/**
				 * Gueteberechnung
				 */
				try {
					GWert gueteSummand1 = new GWert(minuend.getWert()
							.getGueteIndex(), GueteVerfahren.getZustand(minuend
							.getWert().getVerfahren()), false);

					GWert gueteSummand2 = new GWert(subtrahend.getWert()
							.getGueteIndex(),
							GueteVerfahren.getZustand(subtrahend.getWert()
									.getVerfahren()), false);

					GWert gueteGesamt = GueteVerfahren.differenz(
							GueteVerfahren.gewichte(gueteSummand1,
									Math.abs(minuend.getAnteil())),
							GueteVerfahren.gewichte(gueteSummand2,
									Math.abs(subtrahend.getAnteil())));
					ergebnis.getWert().getGueteIndex()
							.setWert(gueteGesamt.getIndexUnskaliert());
					ergebnis.getWert().setVerfahren(
							gueteGesamt.getVerfahren().getCode());
				} catch (GueteException e) {
					e.printStackTrace();
					Debug.getLogger()
							.error("Guete-Differenz konnte nicht ermittelt werden.\n***Minuend***\n" + minuend.getWert() + //$NON-NLS-1$
									"\n***Subtrahend***\n"
									+ subtrahend.getWert()); //$NON-NLS-1$
				}
			}
		}

		return ergebnis;
	}

	@Override
	public String toString() {
		return (this.wert == null ? "<<null>>" : this.wert.toString())
				+ " (Anteil: + " + DUAUtensilien.runde(this.anteil, 2) + ")";
	}

}
