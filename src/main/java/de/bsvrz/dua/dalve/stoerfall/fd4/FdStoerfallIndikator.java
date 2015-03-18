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
package de.bsvrz.dua.dalve.stoerfall.fd4;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.dalve.ErfassungsIntervallDauerMQ;
import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;
import de.bsvrz.dua.dalve.stoerfall.AbstraktStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.modell.DatensatzUpdateEvent;
import de.bsvrz.sys.funclib.bitctrl.modell.DatensatzUpdateListener;
import de.bsvrz.sys.funclib.bitctrl.modell.Datum;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.objekte.StoerfallIndikator;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.parameter.PdFundamentalDiagramm;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;
import de.bsvrz.sys.funclib.debug.Debug;

// TODO: Auto-generated Javadoc
/**
 * Repräsentiert einen Stoerfallindikator nach Verfahren Fundamentaldiagramm.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class FdStoerfallIndikator extends AbstraktStoerfallIndikator implements
DatensatzUpdateListener {

	/**
	 * Verkehrsmenge des Fundamentaldiagramms.
	 */
	private double Q0 = -4;

	/**
	 * Maximale Dichte des Fundamentaldiagramms.
	 */
	private double K0 = -4;

	/**
	 * V0-Geschwindigkeit des Fundamentaldiagramms.
	 */
	private double V0 = -4;

	/**
	 * Freie Geschwindigkeit des Fundamentaldiagramms.
	 */
	private double VFrei = -4;

	/**
	 * Fundamentaldiagramm am MQ.
	 */
	private PdFundamentalDiagramm.Daten fdMQ = null;

	/**
	 * Fundamentaldiagramm am Straßenteilsegment.
	 */
	private PdFundamentalDiagramm.Daten fdSts = null;

	/** Faktor für die Ermittlung der Analysedichte. */
	private final double fa = -1;

	/** Faktor für die Ermittlung der Prognosedichte. */
	private final double fp = -1;

	/** Objekt, das die Prognosedichte ermittelt. */
	private KKfzStoerfallGErmittler prognoseDichteObj = null;

	/**
	 * Parameter der Attributgruppe <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 */
	private AtgLokaleStoerfallErkennungFundamentalDiagramm parameterLokal = null;

	/** der Zuatsnd, der zum Zeitpunkt t-T errechnet wurde. */
	private StoerfallSituation alterZustand = StoerfallSituation.KEINE_AUSSAGE;

	/**
	 * Erfassungsintervalldauer.
	 */
	private ErfassungsIntervallDauerMQ erf = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(final ClientDavInterface dav, final SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);

		if (objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			erf = ErfassungsIntervallDauerMQ.getInstanz(dav, objekt);
		}

		final SystemObject stsObjekt = DatenaufbereitungLVE.getStraßenTeilSegment(objekt);
		if (stsObjekt != null) {
			final PdFundamentalDiagramm fdAmSts = new PdFundamentalDiagramm(new StoerfallIndikator(
					stsObjekt));
			fdAmSts.addUpdateListener(this);
			Debug.getLogger()
					.info("Fuer "
							+ objekt
					+ " wird (falls versorgt) das Fundamentaldiagramm am Teilsegment "
					+ stsObjekt
							+ " verwendet. Falls nicht versorgt wird das Fundamentaldiagramm am MQ selbst verwendet");
		} else {
			Debug.getLogger()
			.warning(
					"Fuer "
							+ objekt
							+ " wird nur das Fundamentaldiagramm am MQ selbst verwendet."
							+ " Eigentlich sollte das Fundamentaldiagramm vom assoziierten Strassenteilsegment uebernommen werden, "
							+ "dies konnte aber nicht ermittelt werden.");
		}

		final PdFundamentalDiagramm fdAmMQ = new PdFundamentalDiagramm(new StoerfallIndikator(
				objekt));
		fdAmMQ.addUpdateListener(this);

		prognoseDichteObj = new KKfzStoerfallGErmittler(dav, objekt);
		parameterLokal = new AtgLokaleStoerfallErkennungFundamentalDiagramm(dav, objekt);

		/**
		 * Anmeldung auf Daten (hier Analysedaten)
		 */
		dav.subscribeReceiver(this, objekt,
				new DataDescription(DatenaufbereitungLVE.getAnalyseAtg(this.objekt), dav
				.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)), ReceiveOptions
						.normal(), ReceiverRole.receiver());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenFD"; //$NON-NLS-1$
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Analysedaten analog
	 * SE-02.00.00.00.00-AFo-4.0 (S.160 f) - Fundamentaldiagramm
	 *
	 * @param resultat
	 *            ein empfangenes Analysedatum mit Nutzdaten
	 */
	@Override
	protected void berechneStoerfallIndikator(final ResultData resultat) {
		Data data = null;

		if (resultat.getData() != null) {
			StoerfallSituation stufe = StoerfallSituation.KEINE_AUSSAGE;

			if (alleParameterValide()) {
				data = DAV.createData(pubBeschreibung.getAttributeGroup());

				final AnalyseDichte KKfzStoerfall = getAnalyseDichte(resultat);
				double KKfzStoerfallG = Double.NaN;
				try {
					KKfzStoerfallG = prognoseDichteObj.getKKfzStoerfallGAktuell(
							KKfzStoerfall.getWert(), KKfzStoerfall.isImplausibel());
				} catch (final PrognoseParameterException e) {
					Debug.getLogger().warning(e.getMessage());
				}

				// System.out.println(KKfzStoerfallG);
				if (!Double.isNaN(KKfzStoerfallG)) {
					stufe = StoerfallSituation.FREIER_VERKEHR;
					stufe = berechneStufe(StoerfallSituation.ZAEHER_VERKEHR, KKfzStoerfallG, stufe);
					stufe = berechneStufe(StoerfallSituation.STAU, KKfzStoerfallG, stufe);
				}

				final StoerfallZustand zustand = new StoerfallZustand(DAV);
				if ((erf != null) && (erf.getT() > 0)) {
					zustand.setT(erf.getT());
				}
				zustand.setSituation(stufe);
				data = zustand.getData();
			} else {
				Debug.getLogger().warning(
						"Keine gueltigen Parameter fuer Stoerfallprognose: " + objekt); //$NON-NLS-1$
			}

			alterZustand = stufe;
		}

		final ResultData ergebnis = new ResultData(objekt, pubBeschreibung, resultat.getDataTime(),
				data);
		sendeErgebnis(ergebnis);
	}

	/**
	 * Berechnet, ob die uebergebene Stoerfallsituation gerade anliegt.
	 *
	 * @param stufe
	 *            die Stoerfallsituation, deren Existenz zu ueberpruefen ist
	 * @param KKfzStoerfallG
	 *            das geglaettete Attribut <code>KKfzStoerfall</code>
	 * @param stufeAlt
	 *            die Stoerfallsituation die bereits detektiert wurde
	 * @return die Stoerfallsituation deren Anliegen ueberprueft werden sollte, wenn diese
	 *         tatsaechlich anliegt, oder die Stoerfallsituation die bereits detektiert wurde, sonst
	 */
	private final StoerfallSituation berechneStufe(final StoerfallSituation stufe,
			final double KKfzStoerfallG, final StoerfallSituation stufeAlt) {
		StoerfallSituation ergebnis = stufeAlt;

		final ParameterFuerStoerfall parameter = parameterLokal.getParameterFuerStoerfall(stufe);
		if (parameter.isInitialisiert()) {
			double VKfzStoerfallG = 0;
			if (KKfzStoerfallG < (fp * K0)) {
				if (K0 > 0) {
					VKfzStoerfallG = VFrei - (((VFrei - V0) / K0) * KKfzStoerfallG);
				} else {
					VKfzStoerfallG = VFrei;
				}
			} else {
				if (KKfzStoerfallG != 0) {
					VKfzStoerfallG = (Q0 * K0) / Math.pow(KKfzStoerfallG, 2.0);
				}
			}

			final boolean fkVergleichMachen = parameter.getFk() != 0;
			final boolean fvVergleichMachen = parameter.getFv() != 0;
			final boolean vGrenzVergleichMachen = parameter.getVgrenz() != 0;

			boolean fkVergleichsErgebnis;
			boolean fvVergleichsErgebnis;
			boolean vGrenzVergleichsErgebnis;

			if (alterZustand.equals(stufe)) {
				/**
				 * Ausschalthysterese
				 */
				fkVergleichsErgebnis = KKfzStoerfallG > ((parameter.getFk() - parameter
						.getFkHysterese()) * K0);
				fvVergleichsErgebnis = VKfzStoerfallG < ((parameter.getFv() + parameter
						.getFvHysterese()) * V0);
				vGrenzVergleichsErgebnis = VKfzStoerfallG < (parameter.getVgrenz() + parameter
						.getVgrenzHysterese());
			} else {
				/**
				 * Einschalthysterese
				 */
				fkVergleichsErgebnis = KKfzStoerfallG > ((parameter.getFk() + parameter
						.getFkHysterese()) * K0);
				fvVergleichsErgebnis = VKfzStoerfallG < ((parameter.getFv() - parameter
						.getFvHysterese()) * V0);
				vGrenzVergleichsErgebnis = VKfzStoerfallG < (parameter.getVgrenz() - parameter
						.getVgrenzHysterese());
			}

			if (getErgebnisAusBoolscherFormel(fkVergleichMachen, fvVergleichMachen,
					vGrenzVergleichMachen, fkVergleichsErgebnis, fvVergleichsErgebnis,
					vGrenzVergleichsErgebnis)) {
				ergebnis = stufe;
			}
		}

		return ergebnis;
	}

	/**
	 * Berechnet die boolesche Formel:<br>
	 * <code>
	 * ergebnis := fkVergleichsErgebnis & fvVergleichsErgebnis | vGrenzVergleichsErgebnis
	 * </code><br>
	 * wobei jeweils nur die Teile in der Formel verbleiben, die als "zu machen" uebergeben wurden.
	 *
	 * @param fkVergleichMachen
	 *            Indikator fuer die Existenz des 1. Terms
	 * @param fvVergleichMachen
	 *            Indikator fuer die Existenz des 2. Terms
	 * @param vGrenzVergleichMachen
	 *            Indikator fuer die Existenz des 3. Terms
	 * @param fkVergleichsErgebnis
	 *            Wert des 1. Terms
	 * @param fvVergleichsErgebnis
	 *            Wert des 2. Terms
	 * @param vGrenzVergleichsErgebnis
	 *            Wert des 3. Terms
	 * @return Ergebnis der Verknuepfung der drei Werte ueber die manipulierte Formel
	 */
	private final boolean getErgebnisAusBoolscherFormel(final boolean fkVergleichMachen,
			final boolean fvVergleichMachen, final boolean vGrenzVergleichMachen,
			final boolean fkVergleichsErgebnis, final boolean fvVergleichsErgebnis,
			final boolean vGrenzVergleichsErgebnis) {
		boolean ergebnis;

		if (!fkVergleichMachen && !fvVergleichMachen && !vGrenzVergleichMachen) {
			ergebnis = false;
		} else if (!fkVergleichMachen && !fvVergleichMachen && vGrenzVergleichMachen) {
			ergebnis = vGrenzVergleichsErgebnis;
		} else if (!fkVergleichMachen && fvVergleichMachen && !vGrenzVergleichMachen) {
			ergebnis = fvVergleichsErgebnis;
		} else if (!fkVergleichMachen && fvVergleichMachen && vGrenzVergleichMachen) {
			ergebnis = fvVergleichsErgebnis || vGrenzVergleichsErgebnis;
		} else

			if (fkVergleichMachen && !fvVergleichMachen && !vGrenzVergleichMachen) {
				ergebnis = fkVergleichsErgebnis;
			} else if (fkVergleichMachen && !fvVergleichMachen && vGrenzVergleichMachen) {
				ergebnis = fkVergleichsErgebnis || vGrenzVergleichsErgebnis;
			} else if (fkVergleichMachen && fvVergleichMachen && !vGrenzVergleichMachen) {
				ergebnis = fkVergleichsErgebnis && fvVergleichsErgebnis;
			} else {
				ergebnis = (fkVergleichsErgebnis && fvVergleichsErgebnis) || vGrenzVergleichsErgebnis;
			}

		return ergebnis;
	}

	// /**
	// * Erfragt die Analysedichte zur Störfallerkennung
	// <code>KKfzStoerfall</code>.
	// * Die Berechnung erfolgt analog SE-02.00.00.00.00-AFo-4.0 (siehe
	// 6.6.4.3.2.1.2)
	// *
	// * @param resultat ein Analysedatum des MQs (muss <code> != null</code>
	// sein und
	// * Nutzdaten enthalten)
	// * @return die Analysedichte zur Störfallerkennung
	// <code>KKfzStoerfall</code>
	// */
	// private final double getAnalyseDichte(ResultData resultat){
	// double KKfzStoerfall;
	//
	// double QKfz =
	// resultat.getData().getItem("QKfz").getUnscaledValue("Wert").longValue();
	// //$NON-NLS-1$ //$NON-NLS-2$
	// double VKfz =
	// resultat.getData().getItem("VKfz").getUnscaledValue("Wert").longValue();
	// //$NON-NLS-1$ //$NON-NLS-2$
	// double KKfz =
	// resultat.getData().getItem("KKfz").getUnscaledValue("Wert").longValue();
	// //$NON-NLS-1$ //$NON-NLS-2$
	//
	// if(QKfz == 0){
	// KKfzStoerfall = 0;
	// }else{
	// if(VKfz == 0 || VKfz == DUAKonstanten.NICHT_ERMITTELBAR){
	// KKfzStoerfall = this.K0;
	// }else
	// if(VKfz >= this.fa * this.V0){
	// KKfzStoerfall = KKfz;
	// }else{
	// if(QKfz > 0){
	// KKfzStoerfall = Math.min(K0 * Q0 / QKfz, 2.0 * K0);
	// }else{
	// KKfzStoerfall = 2.0 * K0;
	// }
	// }
	// }
	//
	// return KKfzStoerfall;
	// }

	/**
	 * Erfragt die Analysedichte zur Störfallerkennung <code>KKfzStoerfall</code>. Die Berechnung
	 * erfolgt analog SE-02.00.00.00.00-AFo-4.0 (siehe 6.6.4.3.2.1.2)
	 *
	 * @param resultat
	 *            ein Analysedatum des MQs (muss <code> != null</code> sein und Nutzdaten enthalten)
	 * @return die Analysedichte zur Störfallerkennung <code>KKfzStoerfall</code>
	 */
	private final AnalyseDichte getAnalyseDichte(final ResultData resultat) {
		double KKfzStoerfall;
		boolean implausibel = false;

		final double QKfz = resultat.getData().getItem("QKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		final boolean QKfzImpl = resultat.getData().getItem("QKfz").getItem("Status"). //$NON-NLS-1$ //$NON-NLS-2$
				getItem("MessWertErsetzung").getUnscaledValue("Implausibel").intValue() == DUAKonstanten.JA; //$NON-NLS-1$ //$NON-NLS-2$
		final double VKfz = resultat.getData().getItem("VKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		final double KKfz = resultat.getData().getItem("KKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		final boolean KKfzImpl = resultat.getData().getItem("KKfz").getItem("Status"). //$NON-NLS-1$ //$NON-NLS-2$
				getItem("MessWertErsetzung").getUnscaledValue("Implausibel").intValue() == DUAKonstanten.JA; //$NON-NLS-1$ //$NON-NLS-2$

		if (QKfz == 0) {
			KKfzStoerfall = 0;
		} else {
			if ((VKfz == 0) || (VKfz == DUAKonstanten.NICHT_ERMITTELBAR)) {
				KKfzStoerfall = K0;
			} else if (VKfz >= (fa * V0)) {
				KKfzStoerfall = KKfz;
				implausibel = KKfzImpl;
			} else {
				if (QKfz > 0) {
					KKfzStoerfall = Math.min((K0 * Q0) / QKfz, 2.0 * K0);
					implausibel = QKfzImpl;
				} else {
					KKfzStoerfall = 2.0 * K0;
				}
			}
		}

		return new AnalyseDichte(KKfzStoerfall, implausibel);
	}

	/**
	 * Klasse zur Speicherung der Analysedichte mit dem Flag <code>implausibel</code>.
	 */
	protected class AnalyseDichte {

		/** der Wert. */
		private double wert = Double.NaN;

		/** Zeigt an, ob der Wert als <code>implausibel</code> gekennzeichnet ist. */
		private boolean implausibel = false;

		/**
		 * Standardkonstruktor.
		 *
		 * @param wert
		 *            der Wert
		 * @param implausibel
		 *            ob der Wert als <code>implausibel</code> gekennzeichnet ist
		 */
		protected AnalyseDichte(final double wert, final boolean implausibel) {
			this.wert = wert;
			this.implausibel = implausibel;
		}

		/**
		 * Erfragt den Wert.
		 *
		 * @return der Wert
		 */
		public final double getWert() {
			return wert;
		}

		/**
		 * Erfragt, ob der Wert als <code>implausibel</code> gekennzeichnet ist.
		 *
		 * @return ob der Wert als <code>implausibel</code> gekennzeichnet ist
		 */
		public final boolean isImplausibel() {
			return implausibel;
		}
	}

	/**
	 * Erfragt, ob bereits alle Parameter initialisiert wurden und sie auf gültigen (verarbeitbaren)
	 * Werten stehen.
	 *
	 * @return ob bereits alle Parameter initialisiert wurden und sie auf gültigen (verarbeitbaren)
	 *         Werten stehen
	 */
	private final boolean alleParameterValide() {
		return parameterLokal.alleParameterInitialisiert() && (Q0 >= 0) && (K0 >= 0) && (V0 >= 0)
				&& (VFrei >= 0);
	}

	/**
	 * wird nicht gebraucht.
	 *
	 * @return the parameter atg pid
	 */
	@Override
	protected String getParameterAtgPid() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readParameter(final ResultData parameter) {
		// wird nicht gebraucht
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void datensatzAktualisiert(final DatensatzUpdateEvent event) {
		if (event.getObjekt() != null) {
			if (event.getObjekt().equals(objekt)) {
				/**
				 * Fundamentaldiagramm am MQ
				 */
				if (event.getDatum().isValid() && (event.getDatensatz() != null)
						&& (event.getDatum().getDatenStatus() == Datum.Status.DATEN)) {
					fdMQ = (PdFundamentalDiagramm.Daten) event.getDatum();
				} else {
					fdMQ = null;
				}
			} else {
				/**
				 * Fundamentaldiagramm am Straßenteilsegment
				 */
				if (event.getDatum().isValid() && (event.getDatensatz() != null)
						&& (event.getDatum().getDatenStatus() == Datum.Status.DATEN)) {
					fdSts = (PdFundamentalDiagramm.Daten) event.getDatum();
				} else {
					fdSts = null;
				}
			}
		}

		if (fdSts != null) {
			Q0 = fdSts.getQ0();
			K0 = fdSts.getK0();
			V0 = fdSts.getV0();
			VFrei = fdSts.getVFrei();
			return;
		}
		if (fdMQ != null) {
			Q0 = fdMQ.getQ0();
			K0 = fdMQ.getK0();
			V0 = fdMQ.getV0();
			VFrei = fdMQ.getVFrei();
			return;
		}

		Q0 = -4;
		K0 = -4;
		V0 = -4;
		VFrei = -4;
	}

}
