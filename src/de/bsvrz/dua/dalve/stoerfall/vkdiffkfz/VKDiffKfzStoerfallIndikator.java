/**
 * Segment 4 Daten¸bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Weiﬂenfelser Straﬂe 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */

package de.bsvrz.dua.dalve.stoerfall.vkdiffkfz;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.dalve.stoerfall.AbstraktStoerfallIndikator;
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

	// private long tReise =

	/**
	 * Daten des Fundamentaldiagramms des Einfahrtsquerschnitts.
	 */
	private PdFundamentalDiagramm.Daten fde = null;

	/**
	 * Daten des Fundamentaldiagramms des Ausfahrtsquerschnitts.
	 */
	private PdFundamentalDiagramm.Daten fda = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(ClientDavInterface dav, SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);

		Data konfigData = objekt.getConfigurationData(dav.getDataModel()
				.getAttributeGroup("atg.straﬂenAbschnitt"));

		SystemObject von = null;
		SystemObject bis = null;

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
										+ "da kein Einfahrtsmessquerschnitt konfiguriert wurde (atg.straﬂenAbschnitt)");
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
										+ "da kein Ausfahrtsmessquerschnitt konfiguriert wurde (atg.straﬂenAbschnitt)");
			}
		} else {
			this.abmelden();
			Debug
					.getLogger()
					.warning(
							"Stoerfallindikator VKDiffKfz kann fuer "
									+ objekt
									+ " nicht ermittelt werden, "
									+ "da keine Ein- und Ausfahrtsmessquerschnitte konfiguriert wurden (atg.straﬂenAbschnitt)");
		}

		if (von != null && bis != null) {
			dav.subscribeReceiver(this, new SystemObject[] { von, bis },
					new DataDescription(dav.getDataModel().getAttributeGroup(
							DUAKonstanten.ATG_KURZZEIT_MQ), dav.getDataModel()
							.getAspect(DUAKonstanten.ASP_ANALYSE)),
					ReceiveOptions.normal(), ReceiverRole.receiver());

			SystemObject fdObjektVon = von;
			SystemObject stsObjektVon = DatenaufbereitungLVE
					.getStraﬂenTeilSegment(von);
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
							&& event.getDatensatz() != null) {
						VKDiffKfzStoerfallIndikator.this.fde = (PdFundamentalDiagramm.Daten) event
								.getDatum();
					}
				}

			});

			SystemObject fdObjektBis = von;
			SystemObject stsObjektBis = DatenaufbereitungLVE
					.getStraﬂenTeilSegment(von);
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
							&& event.getDatensatz() != null) {
						VKDiffKfzStoerfallIndikator.this.fda = (PdFundamentalDiagramm.Daten) event
								.getDatum();
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
		return "atg.lokaleStˆrfallErkennungVKDiffKfz";
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
			} else {
				this.vKDiffEin = -4;
				this.vKDiffAus = -4;
				this.qKfzDiffEin = -4;
				this.qKfzDiffAus = -4;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.stˆrfallVerfahrenVKDiffKfz"; //$NON-NLS-1$
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten
	 * analog MARZ 2004 (siehe 2.3.2.1.4 Verkehrssituationsuebersicht)
	 * 
	 * @param resultat
	 *            ein empfangenes geglaettes Datum mit Nutzdaten
	 */
	protected void berechneStoerfallIndikator(ResultData resultat) {
		Data data = null;

		System.out.println("VKDiffKfz: " + resultat);
	}

}
