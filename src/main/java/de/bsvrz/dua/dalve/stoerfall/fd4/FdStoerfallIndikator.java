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
package de.bsvrz.dua.dalve.stoerfall.fd4;

import de.bsvrz.dav.daf.main.*;
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

/**
 * Repräsentiert einen Stoerfallindikator nach Verfahren Fundamentaldiagramm
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
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

	/**
	 * Faktor für die Ermittlung der Analysedichte
	 */
	private double fa = -1;

	/**
	 * Faktor für die Ermittlung der Prognosedichte
	 */
	private double fp = -1;

	/**
	 * Objekt, das die Prognosedichte ermittelt
	 */
	private KKfzStoerfallGErmittler prognoseDichteObj = null;

	/**
	 * Parameter der Attributgruppe
	 * <code>atg.lokaleStörfallErkennungFundamentalDiagramm</code>
	 */
	private AtgLokaleStoerfallErkennungFundamentalDiagramm parameterLokal = null;

	/**
	 * der Zuatsnd, der zum Zeitpunkt t-T errechnet wurde
	 */
	private StoerfallSituation alterZustand = StoerfallSituation.KEINE_AUSSAGE;
	
	/**
	 * Erfassungsintervalldauer.
	 */
	private ErfassungsIntervallDauerMQ erf = null;

	@Override
	public void initialisiere(ClientDavInterface dav, SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);

		if(objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			this.erf = ErfassungsIntervallDauerMQ.getInstanz(dav, objekt); 
		}
		
		SystemObject stsObjekt = DatenaufbereitungLVE
				.getStraßenTeilSegment(objekt);
		if (stsObjekt != null) {
			PdFundamentalDiagramm fdAmSts = new PdFundamentalDiagramm(
					new StoerfallIndikator(stsObjekt));
			fdAmSts.addUpdateListener(this);
			Debug.getLogger().info(
					"Fuer " + objekt
							+ " wird (falls versorgt) das Fundamentaldiagramm am Teilsegment "
							+ stsObjekt + " verwendet. Falls nicht versorgt wird das Fundamentaldiagramm am MQ selbst verwendet");
		} else {
			Debug
					.getLogger()
					.warning(
							"Fuer "
									+ objekt
									+ " wird nur das Fundamentaldiagramm am MQ selbst verwendet."
									+ " Eigentlich sollte das Fundamentaldiagramm vom assoziierten Strassenteilsegment uebernommen werden, "
									+ "dies konnte aber nicht ermittelt werden.");
		}

		PdFundamentalDiagramm fdAmMQ = new PdFundamentalDiagramm(
				new StoerfallIndikator(objekt));
		fdAmMQ.addUpdateListener(this);

		this.prognoseDichteObj = new KKfzStoerfallGErmittler(dav, objekt);
		this.parameterLokal = new AtgLokaleStoerfallErkennungFundamentalDiagramm(
				dav, objekt);

		/**
		 * Anmeldung auf Daten (hier Analysedaten)
		 */
		dav.subscribeReceiver(this, objekt, new DataDescription(
				DatenaufbereitungLVE.getAnalyseAtg(this.objekt), dav
						.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenFD"; //$NON-NLS-1$
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen
	 * Analysedaten analog SE-02.00.00.00.00-AFo-4.0 (S.160 f) -
	 * Fundamentaldiagramm
	 * 
	 * @param resultat
	 *            ein empfangenes Analysedatum mit Nutzdaten
	 */
	protected void berechneStoerfallIndikator(ResultData resultat) {
		Data data = null;

		if (resultat.getData() != null) {
			StoerfallSituation stufe = StoerfallSituation.KEINE_AUSSAGE;

			if (this.alleParameterValide()) {
				data = DAV.createData(this.pubBeschreibung.getAttributeGroup());
				
				AnalyseDichte KKfzStoerfall = this.getAnalyseDichte(resultat);
				double KKfzStoerfallG = Double.NaN;
				try {
					KKfzStoerfallG = this.prognoseDichteObj
							.getKKfzStoerfallGAktuell(KKfzStoerfall.getWert(),
									KKfzStoerfall.isImplausibel());
				} catch (PrognoseParameterException e) {
					Debug.getLogger().warning(e.getMessage());
				}

				//System.out.println(KKfzStoerfallG);
				if (!Double.isNaN(KKfzStoerfallG)) {
					stufe = StoerfallSituation.FREIER_VERKEHR;
					stufe = berechneStufe(StoerfallSituation.ZAEHER_VERKEHR,
							KKfzStoerfallG, stufe);
					stufe = berechneStufe(StoerfallSituation.STAU,
							KKfzStoerfallG, stufe);
				}
				
				StoerfallZustand zustand = new StoerfallZustand(DAV);
				if(this.erf != null && this.erf.getT() > 0) {
					zustand.setT(this.erf.getT());
				}
				zustand.setSituation(stufe);
				data = zustand.getData();
			} else {
				Debug
						.getLogger()
						.warning(
								"Keine gueltigen Parameter fuer Stoerfallprognose: " + this.objekt); //$NON-NLS-1$
			}

			this.alterZustand = stufe;
		}

		ResultData ergebnis = new ResultData(this.objekt, this.pubBeschreibung,
				resultat.getDataTime(), data);
		this.sendeErgebnis(ergebnis);
	}

	/**
	 * Berechnet, ob die uebergebene Stoerfallsituation gerade anliegt
	 * 
	 * @param stufe
	 *            die Stoerfallsituation, deren Existenz zu ueberpruefen ist
	 * @param KKfzStoerfallG
	 *            das geglaettete Attribut <code>KKfzStoerfall</code>
	 * @param stufeAlt
	 *            die Stoerfallsituation die bereits detektiert wurde
	 * @return die Stoerfallsituation deren Anliegen ueberprueft werden sollte,
	 *         wenn diese tatsaechlich anliegt, oder die Stoerfallsituation die
	 *         bereits detektiert wurde, sonst
	 */
	private final StoerfallSituation berechneStufe(
			final StoerfallSituation stufe, final double KKfzStoerfallG,
			final StoerfallSituation stufeAlt) {
		StoerfallSituation ergebnis = stufeAlt;

		ParameterFuerStoerfall parameter = this.parameterLokal
				.getParameterFuerStoerfall(stufe);
		if (parameter.isInitialisiert()) {
			double VKfzStoerfallG = 0;
			if (KKfzStoerfallG < this.fp * K0) {
				if (K0 > 0) {
					VKfzStoerfallG = VFrei - ((VFrei - V0) / K0)
							* KKfzStoerfallG;
				} else {
					VKfzStoerfallG = VFrei;
				}
			} else {
				if (KKfzStoerfallG != 0) {
					VKfzStoerfallG = Q0 * K0 / Math.pow(KKfzStoerfallG, 2.0);
				}
			}

			boolean fkVergleichMachen = parameter.getFk() != 0;
			boolean fvVergleichMachen = parameter.getFv() != 0;
			boolean vGrenzVergleichMachen = parameter.getVgrenz() != 0;

			boolean fkVergleichsErgebnis;
			boolean fvVergleichsErgebnis;
			boolean vGrenzVergleichsErgebnis;

			if (this.alterZustand.equals(stufe)) {
				/**
				 * Ausschalthysterese
				 */
				fkVergleichsErgebnis = KKfzStoerfallG > (parameter.getFk() - parameter
						.getFkHysterese())
						* K0;
				fvVergleichsErgebnis = VKfzStoerfallG < (parameter.getFv() + parameter
						.getFvHysterese())
						* V0;
				vGrenzVergleichsErgebnis = VKfzStoerfallG < (parameter
						.getVgrenz() + parameter.getVgrenzHysterese());
			} else {
				/**
				 * Einschalthysterese
				 */
				fkVergleichsErgebnis = KKfzStoerfallG > (parameter.getFk() + parameter
						.getFkHysterese())
						* K0;
				fvVergleichsErgebnis = VKfzStoerfallG < (parameter.getFv() - parameter
						.getFvHysterese())
						* V0;
				vGrenzVergleichsErgebnis = VKfzStoerfallG < (parameter
						.getVgrenz() - parameter.getVgrenzHysterese());
			}

			if (this.getErgebnisAusBoolscherFormel(fkVergleichMachen,
					fvVergleichMachen, vGrenzVergleichMachen,
					fkVergleichsErgebnis, fvVergleichsErgebnis,
					vGrenzVergleichsErgebnis)) {
				ergebnis = stufe;
			}
		}

		return ergebnis;
	}

	/**
	 * Berechnet die boolesche Formel:<br>
	 * <code>
	 * ergebnis := fkVergleichsErgebnis &amp; fvVergleichsErgebnis | vGrenzVergleichsErgebnis
	 * </code><br>
	 * wobei jeweils nur die Teile in der Formel verbleiben, die als "zu machen"
	 * uebergeben wurden
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
	 * @return Ergebnis der Verknuepfung der drei Werte ueber die manipulierte
	 *         Formel
	 */
	private final boolean getErgebnisAusBoolscherFormel(
			boolean fkVergleichMachen, boolean fvVergleichMachen,
			boolean vGrenzVergleichMachen, boolean fkVergleichsErgebnis,
			boolean fvVergleichsErgebnis, boolean vGrenzVergleichsErgebnis) {
		boolean ergebnis;

		if (!fkVergleichMachen && !fvVergleichMachen && !vGrenzVergleichMachen) {
			ergebnis = false;
		} else if (!fkVergleichMachen && !fvVergleichMachen
				&& vGrenzVergleichMachen) {
			ergebnis = vGrenzVergleichsErgebnis;
		} else if (!fkVergleichMachen && fvVergleichMachen
				&& !vGrenzVergleichMachen) {
			ergebnis = fvVergleichsErgebnis;
		} else if (!fkVergleichMachen && fvVergleichMachen
				&& vGrenzVergleichMachen) {
			ergebnis = fvVergleichsErgebnis || vGrenzVergleichsErgebnis;
		} else

		if (fkVergleichMachen && !fvVergleichMachen && !vGrenzVergleichMachen) {
			ergebnis = fkVergleichsErgebnis;
		} else if (fkVergleichMachen && !fvVergleichMachen
				&& vGrenzVergleichMachen) {
			ergebnis = fkVergleichsErgebnis || vGrenzVergleichsErgebnis;
		} else if (fkVergleichMachen && fvVergleichMachen
				&& !vGrenzVergleichMachen) {
			ergebnis = fkVergleichsErgebnis && fvVergleichsErgebnis;
		} else {
			ergebnis = fkVergleichsErgebnis && fvVergleichsErgebnis
					|| vGrenzVergleichsErgebnis;
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
	 * Erfragt die Analysedichte zur Störfallerkennung
	 * <code>KKfzStoerfall</code>. Die Berechnung erfolgt analog
	 * SE-02.00.00.00.00-AFo-4.0 (siehe 6.6.4.3.2.1.2)
	 * 
	 * @param resultat
	 *            ein Analysedatum des MQs (muss <code> != null</code> sein und
	 *            Nutzdaten enthalten)
	 * @return die Analysedichte zur Störfallerkennung
	 *         <code>KKfzStoerfall</code>
	 */
	private final AnalyseDichte getAnalyseDichte(ResultData resultat) {
		double KKfzStoerfall;
		boolean implausibel = false;

		double QKfz = resultat.getData()
				.getItem("QKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		boolean QKfzImpl = resultat
				.getData()
				.getItem("QKfz").getItem("Status"). //$NON-NLS-1$ //$NON-NLS-2$
				getItem("MessWertErsetzung").getUnscaledValue("Implausibel").intValue() == DUAKonstanten.JA; //$NON-NLS-1$ //$NON-NLS-2$
		double VKfz = resultat.getData()
				.getItem("VKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		double KKfz = resultat.getData()
				.getItem("KKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
		boolean KKfzImpl = resultat
				.getData()
				.getItem("KKfz").getItem("Status"). //$NON-NLS-1$ //$NON-NLS-2$
				getItem("MessWertErsetzung").getUnscaledValue("Implausibel").intValue() == DUAKonstanten.JA; //$NON-NLS-1$ //$NON-NLS-2$

		if (QKfz == 0) {
			KKfzStoerfall = 0;
		} else {
			if (VKfz == 0 || VKfz == DUAKonstanten.NICHT_ERMITTELBAR) {
				KKfzStoerfall = this.K0;
			} else if (VKfz >= this.fa * this.V0) {
				KKfzStoerfall = KKfz;
				implausibel = KKfzImpl;
			} else {
				if (QKfz > 0) {
					KKfzStoerfall = Math.min(K0 * Q0 / QKfz, 2.0 * K0);
					implausibel = QKfzImpl;
				} else {
					KKfzStoerfall = 2.0 * K0;
				}
			}
		}

		return new AnalyseDichte(KKfzStoerfall, implausibel);
	}

	/**
	 * Klasse zur Speicherung der Analysedichte mit dem Flag
	 * <code>implausibel</code>
	 */
	protected class AnalyseDichte {

		/**
		 * der Wert
		 */
		private double wert = Double.NaN;

		/**
		 * Zeigt an, ob der Wert als <code>implausibel</code> gekennzeichnet
		 * ist
		 */
		private boolean implausibel = false;

		/**
		 * Standardkonstruktor
		 * 
		 * @param wert
		 *            der Wert
		 * @param implausibel
		 *            ob der Wert als <code>implausibel</code> gekennzeichnet
		 *            ist
		 */
		protected AnalyseDichte(double wert, boolean implausibel) {
			this.wert = wert;
			this.implausibel = implausibel;
		}

		/**
		 * Erfragt den Wert
		 * 
		 * @return der Wert
		 */
		public final double getWert() {
			return this.wert;
		}

		/**
		 * Erfragt, ob der Wert als <code>implausibel</code> gekennzeichnet
		 * ist
		 * 
		 * @return ob der Wert als <code>implausibel</code> gekennzeichnet ist
		 */
		public final boolean isImplausibel() {
			return this.implausibel;
		}
	}

	/**
	 * Erfragt, ob bereits alle Parameter initialisiert wurden und sie auf
	 * gültigen (verarbeitbaren) Werten stehen
	 * 
	 * @return ob bereits alle Parameter initialisiert wurden und sie auf
	 *         gültigen (verarbeitbaren) Werten stehen
	 */
	private final boolean alleParameterValide() {
		return this.parameterLokal.alleParameterInitialisiert() && this.Q0 >= 0
				&& this.K0 >= 0 && this.V0 >= 0 && this.VFrei >= 0;
	}

	/**
	 * wird nicht gebraucht
	 */
	@Override
	protected String getParameterAtgPid() {
		return null;
	}

	@Override
	protected void readParameter(ResultData parameter) {
		// wird nicht gebraucht
	}

	public void datensatzAktualisiert(DatensatzUpdateEvent event) {
		if(event.getObjekt() != null) {
			if(event.getObjekt().equals(this.objekt)) {
				/**
				 * Fundamentaldiagramm am MQ
				 */
				if (event.getDatum().isValid() && event.getDatensatz() != null && event
						.getDatum().getDatenStatus() == Datum.Status.DATEN) {
					this.fdMQ = (PdFundamentalDiagramm.Daten) event
					.getDatum();
				}else{
					this.fdMQ = null;
				}
			}else {
				/**
				 * Fundamentaldiagramm am Straßenteilsegment
				 */
				if (event.getDatum().isValid() && event.getDatensatz() != null && event
						.getDatum().getDatenStatus() == Datum.Status.DATEN) {
					this.fdSts = (PdFundamentalDiagramm.Daten) event
					.getDatum();
				}else{
					this.fdSts = null;
				}
			}		
		}
		
		if(this.fdSts != null) {
			this.Q0 = this.fdSts.getQ0();
			this.K0 = this.fdSts.getK0();
			this.V0 = this.fdSts.getV0();
			this.VFrei = this.fdSts.getVFrei();			
			return;
		}
		if(this.fdMQ != null) {
			this.Q0 = this.fdMQ.getQ0();
			this.K0 = this.fdMQ.getK0();
			this.V0 = this.fdMQ.getV0();
			this.VFrei = this.fdMQ.getVFrei();			
			return;
		}
		
		this.Q0 = -4;
		this.K0 = -4;
		this.V0 = -4;
		this.VFrei = -4;
	}

}
