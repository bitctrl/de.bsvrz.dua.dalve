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

package de.bsvrz.dua.dalve.stoerfall.vkdiffkfz;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
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
 */
public class VKDiffKfzStoerfallIndikator extends AbstraktStoerfallIndikator {

	private static final Debug LOGGER = Debug.getLogger();

	/**
	 * Letzter von irgend einer Instanz dieser Klasse errechneter Wert <code>VKDiffKfz</code>. Nur
	 * fuer Testzwecke.
	 */
	private static double aktuellesVkDiffKfz = Double.NaN;

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
	private final RingPuffer vKfzEPuffer = new RingPuffer();

	/**
	 * Ringpuffer fuer kKfz(e).
	 */
	private final RingPuffer kKfzEPuffer = new RingPuffer();

	/**
	 * Aktueller Wert fuer VKfz(a).
	 */
	private VKDiffWert vKfzAAktuell = VKDiffWert.getLeer(System.currentTimeMillis());

	/**
	 * Aktueller Wert fuer KKfz(a).
	 */
	private VKDiffWert kKfzAAktuell = VKDiffWert.getLeer(System.currentTimeMillis());

	/**
	 * Aktueller Wert fuer QKfz(e).
	 */
	private VKDiffWert qKfzEAktuell = VKDiffWert.getLeer(System.currentTimeMillis());

	/**
	 * Die im Schritt <code>t-T</code> ermittelte Stoerfallsituation.
	 */
	private StoerfallSituation alteSituation = FREI;

	/**
	 * Die im Schritt <code>t-T</code> ermittelte Guete der Stoerfallsituation.
	 */
	private GWert alteGuete = GWert.getNichtErmittelbareGuete(GueteVerfahren.STANDARD);

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

	@Override
	public void initialisiere(final ClientDavInterface dav, final SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);

		final Data konfigData = objekt
				.getConfigurationData(dav.getDataModel().getAttributeGroup("atg.straßenAbschnitt"));

		if (konfigData != null) {
			if ((konfigData.getReferenceValue("vonMessQuerschnitt") != null) && (konfigData
					.getReferenceValue("vonMessQuerschnitt").getSystemObject() != null)) {
				von = konfigData.getReferenceValue("vonMessQuerschnitt").getSystemObject();
			} else {
				abmelden();
				LOGGER.warning("Stoerfallindikator VKDiffKfz kann fuer " + objekt
						+ " nicht ermittelt werden, "
						+ "da kein Einfahrtsmessquerschnitt konfiguriert wurde (atg.straßenAbschnitt)");
			}
			if ((konfigData.getReferenceValue("bisMessQuerschnitt") != null) && (konfigData
					.getReferenceValue("bisMessQuerschnitt").getSystemObject() != null)) {
				bis = konfigData.getReferenceValue("bisMessQuerschnitt").getSystemObject();
			} else {
				abmelden();
				LOGGER.warning("Stoerfallindikator VKDiffKfz kann fuer " + objekt
						+ " nicht ermittelt werden, "
						+ "da kein Ausfahrtsmessquerschnitt konfiguriert wurde (atg.straßenAbschnitt)");
			}
		} else {
			abmelden();
			LOGGER.warning("Stoerfallindikator VKDiffKfz kann fuer " + objekt
					+ " nicht ermittelt werden, "
					+ "da keine Ein- und Ausfahrtsmessquerschnitte konfiguriert wurden (atg.straßenAbschnitt)");
		}

		if ((von != null) && (bis != null)) {
			vonT = ErfassungsIntervallDauerMQ.getInstanz(dav, von);
			bisT = ErfassungsIntervallDauerMQ.getInstanz(dav, bis);

			dav.subscribeReceiver(this, new SystemObject[] { von, bis },
					new DataDescription(
							dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ),
							dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)),
							ReceiveOptions.normal(), ReceiverRole.receiver());

			SystemObject fdObjektVon = von;
			final SystemObject stsObjektVon = DatenaufbereitungLVE.getStraßenTeilSegment(von);
			if (stsObjektVon != null) {
				fdObjektVon = stsObjektVon;
				LOGGER.info("Fuer " + objekt + " wird das Fundamentaldiagramm am Teilsegment "
						+ stsObjektVon + " verwendet");
			} else {
				LOGGER.warning(
						"Fuer " + objekt + " wird das Fundamentaldiagramm am MQ selbst verwendet."
								+ " Eigentlich sollte das Fundamentaldiagramm vom assoziierten Strassenteilsegment uebernommen werden, "
								+ "dies konnte aber nicht ermittelt werden.");
			}

			final PdFundamentalDiagramm fdVon = new PdFundamentalDiagramm(
					new StoerfallIndikator(fdObjektVon));
			fdVon.addUpdateListener(new DatensatzUpdateListener() {

				@Override
				public void datensatzAktualisiert(final DatensatzUpdateEvent event) {
					if (event.getDatum().isValid() && (event.getDatensatz() != null)
							&& (event.getDatum() != null)
							&& (event.getDatum().getDatenStatus() == Datum.Status.DATEN)) {
						final PdFundamentalDiagramm.Daten fde = (PdFundamentalDiagramm.Daten) event
								.getDatum();
						if (fde.getK0() >= 0) {
							k0E = fde.getK0();
						} else {
							k0E = Double.NaN;
						}
						if (fde.getVFrei() >= 0) {
							vFreiE = fde.getVFrei();
						} else {
							vFreiE = Double.NaN;
						}
					} else {
						k0E = Double.NaN;
						vFreiE = Double.NaN;
					}
				}

			});

			SystemObject fdObjektBis = bis;
			final SystemObject stsObjektBis = DatenaufbereitungLVE.getStraßenTeilSegment(bis);
			if (stsObjektBis != null) {
				fdObjektBis = stsObjektBis;
				LOGGER.info("Fuer " + objekt + " wird das Fundamentaldiagramm am Teilsegment "
						+ stsObjektBis + " verwendet");
			} else {
				LOGGER.warning(
						"Fuer " + objekt + " wird das Fundamentaldiagramm am MQ selbst verwendet."
								+ " Eigentlich sollte das Fundamentaldiagramm vom assoziierten Strassenteilsegment uebernommen werden, "
								+ "dies konnte aber nicht ermittelt werden.");
			}

			final PdFundamentalDiagramm fdBis = new PdFundamentalDiagramm(
					new StoerfallIndikator(fdObjektBis));
			fdBis.addUpdateListener(new DatensatzUpdateListener() {

				@Override
				public void datensatzAktualisiert(final DatensatzUpdateEvent event) {
					if (event.getDatum().isValid() && (event.getDatensatz() != null)
							&& (event.getDatum() != null)
							&& (event.getDatum().getDatenStatus() == Datum.Status.DATEN)) {
						final PdFundamentalDiagramm.Daten fda = (PdFundamentalDiagramm.Daten) event
								.getDatum();
						if (fda.getK0() >= 0) {
							k0A = fda.getK0();
						} else {
							k0A = Double.NaN;
						}
						if (fda.getVFrei() >= 0) {
							vFreiA = fda.getVFrei();
						} else {
							vFreiA = Double.NaN;
						}
					} else {
						k0A = Double.NaN;
						vFreiA = Double.NaN;
					}
				}

			});
		}
	}

	@Override
	protected String getParameterAtgPid() {
		return "atg.lokaleStörfallErkennungVKDiffKfz";
	}

	@Override
	protected void readParameter(final ResultData parameter) {
		if (parameter != null) {
			if (parameter.getData() != null) {
				vKDiffEin = parameter.getData().getItem("VKDiffKfz").getUnscaledValue("Ein")
						.longValue();
				vKDiffAus = parameter.getData().getItem("VKDiffKfz").getUnscaledValue("Aus")
						.longValue();
				qKfzDiffEin = parameter.getData().getItem("QKfzDiff").getUnscaledValue("Ein")
						.longValue();
				qKfzDiffAus = parameter.getData().getItem("QKfzDiff").getUnscaledValue("Aus")
						.longValue();

				final long tReiseDummy = parameter.getData().getItem("tReise").asUnscaledValue()
						.longValue() * Constants.MILLIS_PER_SECOND;
				if (tReiseDummy >= 0) {
					tReise = tReiseDummy;
				} else {
					tReise = -4;
				}

				kKfzEPuffer.setGroesse(tReise);
				vKfzEPuffer.setGroesse(tReise);

			} else {
				vKDiffEin = -4;
				vKDiffAus = -4;
				qKfzDiffEin = -4;
				qKfzDiffAus = -4;
				tReise = -4;
			}
		}
	}

	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenVKDiffKfz"; //$NON-NLS-1$
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten analog DUA-50.
	 *
	 * @param resultat
	 *            ein empfangenes Analyse-Datum eines MQ mit Nutzdaten.
	 */
	@Override
	protected void berechneStoerfallIndikator(final ResultData resultat) {
		Data data = null;
		double vKDiffKfz = -1;
		StoerfallSituation situation = alteSituation;
		GWert situationsGuete = alteGuete;

		if (resultat.getData() != null) {
			puffereDaten(resultat);

			if (isNeuerIntervallVergangen()) {
				final double qKfzE = qKfzEAktuell.getWert();
				final GWert qKfzEGuete = qKfzEAktuell.getGWert();

				final double vKfzEtMinustReise = vKfzEPuffer
						.getDatumFuerZeitpunkt(resultat.getDataTime() - tReise).getWert();
				final GWert vKfzEtMinustReiseGuete = vKfzEPuffer
						.getDatumFuerZeitpunkt(resultat.getDataTime() - tReise).getGWert();

				final double kKfzEtMinustReise = kKfzEPuffer
						.getDatumFuerZeitpunkt(resultat.getDataTime() - tReise).getWert();
				final GWert kKfzEtMinustReiseGuete = kKfzEPuffer
						.getDatumFuerZeitpunkt(resultat.getDataTime() - tReise).getGWert();

				final double vKfzAt = vKfzAAktuell.getWert();
				final GWert vKfzAtGuete = vKfzAAktuell.getGWert();

				final double kKfzAt = kKfzAAktuell.getWert();
				final GWert kKfzAtGuete = kKfzAAktuell.getGWert();

				// System.out.println("QKfz(e) = " + qKfzE + ", VKfz(e, t-tr) = " +
				// vKfzEtMinustReise + ", KKfz(e, t-tr) = " + kKfzEtMinustReise + ", VKfz(a, t) = "
				// + vKfzAt + ", KKfz(a, t) = " + kKfzAt);
				if (!Double.isNaN(vFreiA) && !Double.isNaN(vFreiE) && !Double.isNaN(k0A)
						&& !Double.isNaN(k0E) && !Double.isNaN(vKfzEtMinustReise)
						&& !Double.isNaN(kKfzEtMinustReise) && !Double.isNaN(vKfzAt)
						&& !Double.isNaN(kKfzAt) && !Double.isNaN(qKfzE)) {

					/**
					 * d.h., alle zur Berechnung notwendigen Werte sind valide.
					 */

					double vFreiEMinusVKfzEtMinustReise = 0.0;
					if ((vFreiE - vKfzEtMinustReise) >= 0.0) {
						vFreiEMinusVKfzEtMinustReise = vFreiE - vKfzEtMinustReise;
					}
					double vFreiAMinusVKfzAt = 0.0;
					if ((vFreiA - vKfzAt) >= 0.0) {
						vFreiAMinusVKfzAt = vFreiA - vKfzAt;
					}

					GWert vKDiffKfzGuete = GWert.getNichtErmittelbareGuete(GueteVerfahren.STANDARD);
					try {
						vKDiffKfzGuete = GueteVerfahren
								.differenz(
										GueteVerfahren.exp(
												GueteVerfahren.summe(
														GueteVerfahren.exp(vKfzEtMinustReiseGuete,
																2.0),
																GueteVerfahren.exp(kKfzEtMinustReiseGuete, 2.0)),
																0.5),
																GueteVerfahren.exp(
																		GueteVerfahren.summe(GueteVerfahren.exp(vKfzAtGuete, 2.0),
																				GueteVerfahren.exp(kKfzAtGuete, 2.0)),
																				0.5));
					} catch (final GueteException ex) {
						LOGGER.error("Guete von VKDiffKfz fuer " + objekt
								+ " konnte nicht bestimmt werden. Grund:\n" + ex.getMessage());
					}

					vKDiffKfz = Math
							.sqrt(Math.pow(vFreiEMinusVKfzEtMinustReise / vFreiE, 2.0)
									+ Math.pow(kKfzEtMinustReise / (2 * k0E), 2.0))
									- Math.sqrt(Math.pow(vFreiAMinusVKfzAt / vFreiA, 2.0)
											+ Math.pow(kKfzAt / (2 * k0A), 2.0));

					if ((vKDiffEin >= 0) && (qKfzDiffEin >= 0) && (vKDiffAus >= 0)
							&& (qKfzDiffAus >= 0) && (qKfzE >= 0)) {

						boolean neueStufeBerechnet = false;
						if ((vKDiffKfz > vKDiffEin) && (qKfzE > qKfzDiffEin)) {
							situation = STAU;
							neueStufeBerechnet = true;
						}

						if ((vKDiffKfz < vKDiffAus) && (qKfzE < qKfzDiffAus)) {
							situation = FREI;
							neueStufeBerechnet = true;
						}

						if (neueStufeBerechnet) {
							try {
								situationsGuete = GueteVerfahren.summe(vKDiffKfzGuete, qKfzEGuete);
							} catch (final GueteException ex) {
								LOGGER.error("Guete von Stoerfallindikator VKDiffKfz fuer " + objekt
										+ " konnte nicht bestimmt werden. Grund:\n"
										+ ex.getMessage());
							}
						}
					} else {
						/**
						 * Keine Aussage, wenn zwar VKDiffKfz berechnet werden konnte, aber der
						 * Stoerfallzustand aufgrund von fehlenden Parametern nicht ausgerechnet
						 * werden konnte.
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

		aktuellesVkDiffKfz = vKDiffKfz;

		if (vonT.getT() > 0) {
			data = DAV.createData(pubBeschreibung.getAttributeGroup());

			final StoerfallZustand zustand = new StoerfallZustand(DAV);
			zustand.setHorizont(0);
			zustand.setT(vonT.getT());
			zustand.setSituation(situation);
			zustand.setVerfahren(GueteVerfahren.STANDARD);
			final GanzZahl g = GanzZahl.getGueteIndex();
			g.setWert(situationsGuete.getIndexUnskaliert());
			zustand.setGuete(g);
			data = zustand.getData();

			alteSituation = situation;
			alteGuete = situationsGuete;
		}

		final ResultData ergebnis = new ResultData(objekt, pubBeschreibung, resultat.getDataTime(),
				data);

		// System.out.println(((resultat.getDataTime() / Constants.MILLIS_PER_MINUTE) + 2) + ": " +
		// vKDiffKfz);
		sendeErgebnis(ergebnis);
	}

	/**
	 * Erfragt, ob eine neuer Intervall vergangen ist.
	 *
	 * @return ob eine neuer Intervall vergangen ist.
	 */
	private boolean isNeuerIntervallVergangen() {
		if (!qKfzEAktuell.isLeer() && !vKfzAAktuell.isLeer()) {
			return qKfzEAktuell.getZeitStempel() == vKfzAAktuell.getZeitStempel();
		}
		return false;
	}

	/**
	 * Puffert alle relevanten empfangenen Daten.
	 *
	 * @param result
	 *            ein MQ-Datum.
	 */
	private void puffereDaten(final ResultData result) {
		if ((vonT.getT() >= 0) && (vonT.getT() == bisT.getT())) {
			/**
			 * MQs haben die gleiche Erfassungsintervalldauer
			 */
			if (result.getObject().equals(von)) {
				/**
				 * 1. Messquerschnitt
				 */
				if (result.getData() == null) {
					qKfzEAktuell = VKDiffWert.getLeer(result.getDataTime());
					vKfzEPuffer.put(VKDiffWert.getLeer(result.getDataTime()));
					kKfzEPuffer.put(VKDiffWert.getLeer(result.getDataTime()));
				} else {
					qKfzEAktuell = new VKDiffWert(new MesswertUnskaliert("QKfz", result.getData()),
							result.getDataTime(), bisT.getT());
					vKfzEPuffer.put(new VKDiffWert(new MesswertUnskaliert("VKfz", result.getData()),
							result.getDataTime(), bisT.getT()));
					kKfzEPuffer.put(new VKDiffWert(new MesswertUnskaliert("KKfz", result.getData()),
							result.getDataTime(), bisT.getT()));
				}
			} else {
				/**
				 * 2. Messquerschnitt
				 */
				if (result.getData() == null) {
					vKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
					kKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
				} else {
					vKfzAAktuell = new VKDiffWert(new MesswertUnskaliert("VKfz", result.getData()),
							result.getDataTime(), bisT.getT());
					kKfzAAktuell = new VKDiffWert(new MesswertUnskaliert("KKfz", result.getData()),
							result.getDataTime(), bisT.getT());
				}
			}
		} else {
			qKfzEAktuell = VKDiffWert.getLeer(result.getDataTime());
			vKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
			kKfzAAktuell = VKDiffWert.getLeer(result.getDataTime());
		}
	}

	/**
	 * Erfragt letzten von irgend einer Instanz dieser Klasse errechneten Wert
	 * <code>VKDiffKfz</code>. Nur fuer Testzwecke.
	 *
	 * @return letzten von irgend einer Instanz dieser Klasse errechneten Wert
	 *         <code>VKDiffKfz</code>.
	 */
	public static final double getTestVkDiffKfz() {
		return aktuellesVkDiffKfz;
	}
}
