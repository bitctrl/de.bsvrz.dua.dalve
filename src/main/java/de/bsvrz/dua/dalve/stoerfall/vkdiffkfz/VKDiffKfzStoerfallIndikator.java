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

package de.bsvrz.dua.dalve.stoerfall.vkdiffkfz;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.dalve.ErfassungsIntervallDauerMQ;
import de.bsvrz.dua.dalve.stoerfall.AbstraktStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.GanzZahl;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.modell.DatensatzUpdateEvent;
import de.bsvrz.sys.funclib.bitctrl.modell.DatensatzUpdateListener;
import de.bsvrz.sys.funclib.bitctrl.modell.Datum;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.objekte.StoerfallIndikator;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.parameter.PdFundamentalDiagramm;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Repraesentiert einen Stoerfallindikator nach Verfahren VKDiffKfz.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public class VKDiffKfzStoerfallIndikator extends AbstraktStoerfallIndikator {

	/**
	 * VKDiffKfz-Situation <code>freier Verkehr</code>.
	 */
	private static final StoerfallSituation FREI = StoerfallSituation.FREIER_VERKEHR;

	/**
	 * VKDiffKfz-Situation <code>Stau</code>.
	 */
	private static final StoerfallSituation STAU = StoerfallSituation.STAU;

	/**
	 * VKDiffKfz-Situation <code>keine Aussage</code>.
	 */
	private static final StoerfallSituation KEINE_AUSSAGE = StoerfallSituation.KEINE_AUSSAGE;

	/**
	 * Parameter <code>VKDiffEin</code>.
	 */
	private long vKDiffEin = -4;

	/**
	 * Parameter <code>VKDiffAus</code>.
	 */
	private long vKDiffAus = -4;

	/**
	 * Parameter <code>QKfzDiffEin</code>.
	 */
	private long qKfzDiffEin = -4;

	/**
	 * Parameter <code>QKfzDiffAus</code>.
	 */
	private long qKfzDiffAus = -4;

	/**
	 * Freie Geschwindigkeit des Fundamentaldiagramms (Einfahrt).
	 */
	private double vFreiE = Double.NaN;

	/**
	 * Maximale Dichte des Fundamentaldiagramms (Einfahrt).
	 */
	private double k0E = Double.NaN;

	/**
	 * Freie Geschwindigkeit des Fundamentaldiagramms (Ausfahrt).
	 */
	private double vFreiA = Double.NaN;

	/**
	 * Maximale Dichte des Fundamentaldiagramms (Ausfahrt).
	 */
	private double k0A = Double.NaN;

	/**
	 * Parametrierte Reisezeit zwischen den beiden MQs.
	 */
	private long tReise = -4;

	/**
	 * Ringpuffer fuer VKfz(e).
	 */
	private RingPuffer vKfzEPuffer = new RingPuffer();;

	/**
	 * Ringpuffer fuer kKfz(e).
	 */
	private RingPuffer kKfzEPuffer = new RingPuffer();;

	/**
	 * Aktueller Wert fuer VKfz(a).
	 */
	private VKDiffWert vKfzAAktuell = VKDiffWert.getLeer(System
			.currentTimeMillis());

	/**
	 * Aktueller Wert fuer KKfz(a).
	 */
	private VKDiffWert kKfzAAktuell = VKDiffWert.getLeer(System
			.currentTimeMillis());

	/**
	 * Aktueller Wert fuer QKfz(e).
	 */
	private VKDiffWert qKfzEAktuell = VKDiffWert.getLeer(System
			.currentTimeMillis());

	/**
	 * Die im Schritt <code>t-T</code> ermittelte Stoerfallsituation.
	 */
	private StoerfallSituation alteSituation = FREI;

	/**
	 * Die im Schritt <code>t-T</code> ermittelte Guete der
	 * Stoerfallsituation.
	 */
	private GWert alteGuete = GWert
			.getNichtErmittelbareGuete(GueteVerfahren.STANDARD);

	/**
	 * 1. Messquerschnitt.
	 */
	private SystemObject von = null;

	/**
	 * 2. Messquerschnitt.
	 */
	private SystemObject bis = null;

	/**
	 * T vom 1. Messquerschnitt.
	 */
	private ErfassungsIntervallDauerMQ vonT = null;

	/**
	 * T vom 2. Messquerschnitt.
	 */
	private ErfassungsIntervallDauerMQ bisT = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(ClientDavInterface dav, SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);

		Data konfigData = objekt.getConfigurationData(dav.getDataModel()
				.getAttributeGroup("atg.straßenAbschnitt"));

		if (konfigData != null) {
			if (konfigData.getReferenceValue("vonMessQuerschnitt") != null
					&& konfigData.getReferenceValue("vonMessQuerschnitt")
							.getSystemObject() != null) {
				von = konfigData.getReferenceValue("vonMessQuerschnitt")
						.getSystemObject();
			} else {
				this.abmelden();
				Debug
						.getLogger()
						.warning(
								"Stoerfallindikator VKDiffKfz kann fuer "
										+ objekt
										+ " nicht ermittelt werden, "
										+ "da kein Einfahrtsmessquerschnitt konfiguriert wurde (atg.straßenAbschnitt)");
			}
			if (konfigData.getReferenceValue("bisMessQuerschnitt") != null
					&& konfigData.getReferenceValue("bisMessQuerschnitt")
							.getSystemObject() != null) {
				bis = konfigData.getReferenceValue("bisMessQuerschnitt")
						.getSystemObject();
			} else {
				this.abmelden();
				Debug
						.getLogger()
						.warning(
								"Stoerfallindikator VKDiffKfz kann fuer "
										+ objekt
										+ " nicht ermittelt werden, "
										+ "da kein Ausfahrtsmessquerschnitt konfiguriert wurde (atg.straßenAbschnitt)");
			}
		} else {
			this.abmelden();
			Debug
					.getLogger()
					.warning(
							"Stoerfallindikator VKDiffKfz kann fuer "
									+ objekt
									+ " nicht ermittelt werden, "
									+ "da keine Ein- und Ausfahrtsmessquerschnitte konfiguriert wurden (atg.straßenAbschnitt)");
		}

		if (von != null && bis != null) {
			this.vonT = ErfassungsIntervallDauerMQ.getInstanz(dav, von);
			this.bisT = ErfassungsIntervallDauerMQ.getInstanz(dav, bis);

			dav.subscribeReceiver(this, new SystemObject[] { von, bis },
					new DataDescription(dav.getDataModel().getAttributeGroup(
							DUAKonstanten.ATG_KURZZEIT_MQ), dav.getDataModel()
							.getAspect(DUAKonstanten.ASP_ANALYSE)),
					ReceiveOptions.normal(), ReceiverRole.receiver());

			SystemObject fdObjektVon = von;
			SystemObject stsObjektVon = DatenaufbereitungLVE
					.getStraßenTeilSegment(von);
			if (stsObjektVon != null) {
				fdObjektVon = stsObjektVon;
				Debug
						.getLogger()
						.info(
								"Fuer "
										+ objekt
										+ " wird das Fundamentaldiagramm am Teilsegment "
										+ stsObjektVon + " verwendet");
			} else {
				Debug
						.getLogger()
						.warning(
								"Fuer "
										+ objekt
										+ " wird das Fundamentaldiagramm am MQ selbst verwendet."
										+ " Eigentlich sollte das Fundamentaldiagramm vom assoziierten Strassenteilsegment uebernommen werden, "
										+ "dies konnte aber nicht ermittelt werden.");
			}

			PdFundamentalDiagramm fdVon = new PdFundamentalDiagramm(
					new StoerfallIndikator(fdObjektVon));
			fdVon.addUpdateListener(new DatensatzUpdateListener() {

				public void datensatzAktualisiert(DatensatzUpdateEvent event) {
					if (event.getDatum().isValid()
							&& event.getDatensatz() != null
							&& event.getDatum() != null
							&& event.getDatum().getDatenStatus() == Datum.Status.DATEN) {
						PdFundamentalDiagramm.Daten fde = (PdFundamentalDiagramm.Daten) event
								.getDatum();
						if (fde.getK0() >= 0) {
							VKDiffKfzStoerfallIndikator.this.k0E = fde.getK0();
						} else {
							VKDiffKfzStoerfallIndikator.this.k0E = Double.NaN;
						}
						if (fde.getVFrei() >= 0) {
							VKDiffKfzStoerfallIndikator.this.vFreiE = fde
									.getVFrei();
						} else {
							VKDiffKfzStoerfallIndikator.this.vFreiE = Double.NaN;
						}
					} else {
						VKDiffKfzStoerfallIndikator.this.k0E = Double.NaN;
						VKDiffKfzStoerfallIndikator.this.vFreiE = Double.NaN;
					}
				}

			});

			SystemObject fdObjektBis = bis;
			SystemObject stsObjektBis = DatenaufbereitungLVE
					.getStraßenTeilSegment(bis);
			if (stsObjektBis != null) {
				fdObjektBis = stsObjektBis;
				Debug
						.getLogger()
						.info(
								"Fuer "
										+ objekt
										+ " wird das Fundamentaldiagramm am Teilsegment "
										+ stsObjektBis + " verwendet");
			} else {
				Debug
						.getLogger()
						.warning(
								"Fuer "
										+ objekt
										+ " wird das Fundamentaldiagramm am MQ selbst verwendet."
										+ " Eigentlich sollte das Fundamentaldiagramm vom assoziierten Strassenteilsegment uebernommen werden, "
										+ "dies konnte aber nicht ermittelt werden.");
			}

			PdFundamentalDiagramm fdBis = new PdFundamentalDiagramm(
					new StoerfallIndikator(fdObjektBis));
			fdBis.addUpdateListener(new DatensatzUpdateListener() {

				public void datensatzAktualisiert(DatensatzUpdateEvent event) {
					if (event.getDatum().isValid()
							&& event.getDatensatz() != null
							&& event.getDatum() != null
							&& event.getDatum().getDatenStatus() == Datum.Status.DATEN) {
						PdFundamentalDiagramm.Daten fda = (PdFundamentalDiagramm.Daten) event
								.getDatum();
						if (fda.getK0() >= 0) {
							VKDiffKfzStoerfallIndikator.this.k0A = fda.getK0();
						} else {
							VKDiffKfzStoerfallIndikator.this.k0A = Double.NaN;
						}
						if (fda.getVFrei() >= 0) {
							VKDiffKfzStoerfallIndikator.this.vFreiA = fda
									.getVFrei();
						} else {
							VKDiffKfzStoerfallIndikator.this.vFreiA = Double.NaN;
						}
					} else {
						VKDiffKfzStoerfallIndikator.this.k0A = Double.NaN;
						VKDiffKfzStoerfallIndikator.this.vFreiA = Double.NaN;
					}
				}

			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParameterAtgPid() {
		return "atg.lokaleStörfallErkennungVKDiffKfz";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readParameter(ResultData parameter) {
		if (parameter != null) {
			if (parameter.getData() != null) {
				this.vKDiffEin = parameter.getData().getItem("VKDiffKfz")
						.getUnscaledValue("Ein").longValue();
				this.vKDiffAus = parameter.getData().getItem("VKDiffKfz")
						.getUnscaledValue("Aus").longValue();
				this.qKfzDiffEin = parameter.getData().getItem("QKfzDiff")
						.getUnscaledValue("Ein").longValue();
				this.qKfzDiffAus = parameter.getData().getItem("QKfzDiff")
						.getUnscaledValue("Aus").longValue();
				
				long tReiseDummy = parameter.getData().getItem("tReise").asUnscaledValue().longValue() * (long) 1000;
				if(tReiseDummy >= 0) {
					this.tReise = tReiseDummy;
				}else{
					tReise = -4;
				}
				
				this.kKfzEPuffer.setGroesse(tReise);
				this.vKfzEPuffer.setGroesse(tReise);
					
			} else {
				this.vKDiffEin = -4;
				this.vKDiffAus = -4;
				this.qKfzDiffEin = -4;
				this.qKfzDiffAus = -4;
				this.tReise = -4;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenVKDiffKfz"; //$NON-NLS-1$
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten
	 * analog DUA-50.
	 * 
	 * @param resultat
	 *            ein empfangenes Analyse-Datum eines MQ mit Nutzdaten.
	 */
	protected void berechneStoerfallIndikator(ResultData resultat) {
		Data data = null;
		double vKDiffKfz = -1;
		StoerfallSituation situation = this.alteSituation;
		GWert situationsGuete = this.alteGuete;

		if (resultat.getData() != null) {
			this.puffereDaten(resultat);

			if (this.isNeuerIntervallVergangen()) {
				double qKfzE = this.qKfzEAktuell.getWert();
				GWert qKfzEGuete = this.qKfzEAktuell.getGWert();

				double vKfzEtMinustReise = this.vKfzEPuffer
						.getDatumFuerZeitpunkt(
								resultat.getDataTime() - this.tReise).getWert();
				GWert vKfzEtMinustReiseGuete = this.vKfzEPuffer
						.getDatumFuerZeitpunkt(
								resultat.getDataTime() - this.tReise)
						.getGWert();

				double kKfzEtMinustReise = this.kKfzEPuffer
						.getDatumFuerZeitpunkt(
								resultat.getDataTime() - this.tReise).getWert();
				GWert kKfzEtMinustReiseGuete = this.kKfzEPuffer
						.getDatumFuerZeitpunkt(
								resultat.getDataTime() - this.tReise)
						.getGWert();

				double vKfzAt = this.vKfzAAktuell.getWert();
				GWert vKfzAtGuete = this.vKfzAAktuell.getGWert();

				double kKfzAt = this.kKfzAAktuell.getWert();
				GWert kKfzAtGuete = this.kKfzAAktuell.getGWert();

				//System.out.println("QKfz(e) = " + qKfzE + ", VKfz(e, t-tr) = " + vKfzEtMinustReise + ", KKfz(e, t-tr) = " + kKfzEtMinustReise + ", VKfz(a, t) = " + vKfzAt + ", KKfz(a, t) = " + kKfzAt);
				if (!Double.isNaN(this.vFreiA) && !Double.isNaN(this.vFreiE)
						&& !Double.isNaN(this.k0A) && !Double.isNaN(this.k0E)
						&& !Double.isNaN(vKfzEtMinustReise)
						&& !Double.isNaN(kKfzEtMinustReise)
						&& !Double.isNaN(vKfzAt) && !Double.isNaN(kKfzAt)
						&& !Double.isNaN(qKfzE)) {
						
					/**
					 * d.h., alle zur Berechnung notwendigen Werte sind valide.
					 */

					double vFreiEMinusVKfzEtMinustReise = 0.0;
					if (this.vFreiE - vKfzEtMinustReise >= 0.0) {
						vFreiEMinusVKfzEtMinustReise = this.vFreiE
								- vKfzEtMinustReise;
					}
					double vFreiAMinusVKfzAt = 0.0;
					if (this.vFreiA - vKfzAt >= 0.0) {
						vFreiAMinusVKfzAt = this.vFreiA - vKfzAt;
					}

					GWert vKDiffKfzGuete = GWert
							.getNichtErmittelbareGuete(GueteVerfahren.STANDARD);
					try {
						vKDiffKfzGuete = GueteVerfahren.differenz(
								GueteVerfahren.exp(GueteVerfahren.summe(
										GueteVerfahren.exp(
												vKfzEtMinustReiseGuete, 2.0),
										GueteVerfahren.exp(
												kKfzEtMinustReiseGuete, 2.0)),
										0.5), GueteVerfahren.exp(GueteVerfahren
										.summe(GueteVerfahren.exp(vKfzAtGuete,
												2.0), GueteVerfahren.exp(
												kKfzAtGuete, 2.0)), 0.5));
					} catch (GueteException ex) {
						Debug
								.getLogger()
								.error(
										"Guete von VKDiffKfz fuer "
												+ this.objekt
												+ " konnte nicht bestimmt werden. Grund:\n"
												+ ex.getMessage());
					}

					vKDiffKfz = Math
							.sqrt(Math.pow(vFreiEMinusVKfzEtMinustReise
									/ this.vFreiE, 2.0)
									+ Math.pow(kKfzEtMinustReise
											/ (2 * this.k0E), 2.0))
							- Math.sqrt(Math.pow(vFreiAMinusVKfzAt
									/ this.vFreiA, 2.0)
									+ Math.pow(kKfzAt / (2 * this.k0A), 2.0));

					if (this.vKDiffEin >= 0 && this.qKfzDiffEin >= 0
							&& this.vKDiffAus >= 0 && this.qKfzDiffAus >= 0
							&& qKfzE >= 0) {

						boolean neueStufeBerechnet = false;
						if (vKDiffKfz > this.vKDiffEin
								&& qKfzE > this.qKfzDiffEin) {
							situation = STAU;
							neueStufeBerechnet = true;
						}

						if (vKDiffKfz < vKDiffAus && qKfzE < qKfzDiffAus) {
							situation = FREI;
							neueStufeBerechnet = true;
						}

						if (neueStufeBerechnet) {
							try {
								situationsGuete = GueteVerfahren.summe(
										vKDiffKfzGuete, qKfzEGuete);
							} catch (GueteException ex) {
								Debug
										.getLogger()
										.error(
												"Guete von Stoerfallindikator VKDiffKfz fuer "
														+ this.objekt
														+ " konnte nicht bestimmt werden. Grund:\n"
														+ ex.getMessage());
							}
						}
					} else {
						/**
						 * Keine Aussage, wenn zwar VKDiffKfz berechnet werden
						 * konnte, aber der Stoerfallzustand aufgrund von
						 * fehlenden Parametern nicht ausgerechnet werden
						 * konnte.
						 */
						situation = KEINE_AUSSAGE;
					}
				}
			} else {
				/**
				 * warte auf Restdatum von anderem MQ
				 */
				return;
			}
		}

		if(this.vonT.getT() > 0){
			data = DAV.createData(this.pubBeschreibung
					.getAttributeGroup());
	
			StoerfallZustand zustand = new StoerfallZustand(DAV);
			zustand.setHorizont(0);
			zustand.setT(this.vonT.getT()); //$NON-NLS-1$
			zustand.setSituation(situation);
			zustand.setVerfahren(GueteVerfahren.STANDARD);
			GanzZahl g = GanzZahl.getGueteIndex();
			g.setWert(situationsGuete.getIndexUnskaliert());
			zustand.setGuete(g);
			data = zustand.getData();
	
			this.alteSituation = situation;
			this.alteGuete = situationsGuete;
		}
			
		ResultData ergebnis = new ResultData(this.objekt, this.pubBeschreibung,
				resultat.getDataTime(), data);
		
		//System.out.println(((resultat.getDataTime() / Constants.MILLIS_PER_MINUTE) + 2) + ": " + vKDiffKfz);
		this.sendeErgebnis(ergebnis);
	}

	/**
	 * Erfragt, ob eine neuer Intervall vergangen ist.
	 * 
	 * @return ob eine neuer Intervall vergangen ist.
	 */
	private boolean isNeuerIntervallVergangen() {
		if (!this.qKfzEAktuell.isLeer() && !this.vKfzAAktuell.isLeer()) {
			return this.qKfzEAktuell.getZeitStempel() == this.vKfzAAktuell
					.getZeitStempel();
		}
		return false;
	}

	/**
	 * Puffert alle relevanten empfangenen Daten.
	 * 
	 * @param result
	 *            ein MQ-Datum.
	 */
	private void puffereDaten(ResultData result) {
		if (this.vonT.getT() >= 0 && this.vonT.getT() == this.bisT.getT()) {
			/**
			 * MQs haben die gleiche Erfassungsintervalldauer
			 */
			if (result.getObject().equals(this.von)) {
				/**
				 * 1. Messquerschnitt
				 */
				if (result.getData() == null) {
					qKfzEAktuell = VKDiffWert.getLeer(result.getDataTime());
					vKfzEPuffer.put(VKDiffWert.getLeer(result.getDataTime()));
					kKfzEPuffer.put(VKDiffWert.getLeer(result.getDataTime()));
				} else {
					qKfzEAktuell = new VKDiffWert(new MesswertUnskaliert(
							"QKfz", result.getData()), result.getDataTime(),
							this.bisT.getT());
					vKfzEPuffer.put(new VKDiffWert(new MesswertUnskaliert(
							"VKfz", result.getData()), result.getDataTime(),
							this.bisT.getT()));
					kKfzEPuffer.put(new VKDiffWert(new MesswertUnskaliert(
							"KKfz", result.getData()), result.getDataTime(),
							this.bisT.getT()));
				}
			} else {
				/**
				 * 2. Messquerschnitt
				 */
				if (result.getData() == null) {
					vKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
					kKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
				} else {
					vKfzAAktuell = new VKDiffWert(new MesswertUnskaliert(
							"VKfz", result.getData()), result.getDataTime(),
							this.bisT.getT());
					kKfzAAktuell = new VKDiffWert(new MesswertUnskaliert(
							"KKfz", result.getData()), result.getDataTime(),
							this.bisT.getT());
				}
			}
		} else {
			qKfzEAktuell = VKDiffWert.getLeer(result.getDataTime());
			vKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
			kKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
		}
	}
}
