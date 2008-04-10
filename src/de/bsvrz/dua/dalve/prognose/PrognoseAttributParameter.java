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
package de.bsvrz.dua.dalve.prognose;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;

/**
 * Korrespondiert mit einem Parameter fuer ein bestimmtes Verkehrsattribut (also
 * z.B. <code>qKfz</code> (<code>QKfz</code>) oder <code>vLkw</code> (<code>VLkw</code>))
 * innerhalb der Attributgruppen:<br> -
 * <code>atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseFlinkFs<code><br>
 * - <code>atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseNormalFs<code><br>
 * - <code>atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseTrägeFs<code><br>
 * - <code>atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseFlinkMq<code><br>
 * - <code>atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseNormalMq<code><br>
 * - <code>atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseTrägeMq<code>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class PrognoseAttributParameter {

	/**
	 * Startwert für die Glaettung
	 */
	private long start = -4;

	/**
	 * Glaettungsparameter für abnehmende Messwerte
	 */
	private double alpha1 = Double.NaN;

	/**
	 * Glaettungsparameter für steigende Messwerte
	 */
	private double alpha2 = Double.NaN;

	/**
	 * Prognoseparameter für abnehmende Messwerte
	 */
	private double beta1 = Double.NaN;

	/**
	 * Prognoseparameter für steigende Messwerte
	 */
	private double beta2 = Double.NaN;

	/**
	 * Das Prognoseattribut, dessen Daten hier gespeichert werden
	 */
	private PrognoseAttribut attribut = null;

	/**
	 * Standardkonstruktor
	 * 
	 * @param attribut
	 *            das Prognoseattribut, dessen Daten hier gespeichert werden
	 */
	public PrognoseAttributParameter(final PrognoseAttribut attribut) {
		if (attribut == null) {
			throw new NullPointerException(
					"Uebergebenes Prognoseattribut ist <<null>>"); //$NON-NLS-1$
		}
		this.attribut = attribut;
	}

	/**
	 * Fuellt dieses Objekt mit Daten
	 * 
	 * @param datum
	 *            Parameterdatum (muss <code>!= null</code> sein)
	 * @param fuerFahrStreifen
	 *            ob die Daten fuer ein Fahrstreifenobjekt ausgelesen werden
	 *            sollen
	 */
	public final void setDaten(final Data datum, final boolean fuerFahrStreifen) {
		this.start = datum.getUnscaledValue(
				attribut.getParameterStart(fuerFahrStreifen)).longValue();
		this.alpha1 = datum.getItem(attribut.getAttributName(fuerFahrStreifen))
				.getScaledValue("alpha1").doubleValue(); //$NON-NLS-1$
		this.alpha2 = datum.getItem(attribut.getAttributName(fuerFahrStreifen))
				.getScaledValue("alpha2").doubleValue(); //$NON-NLS-1$
		this.beta1 = datum.getItem(attribut.getAttributName(fuerFahrStreifen))
				.getScaledValue("beta1").doubleValue(); //$NON-NLS-1$
		this.beta2 = datum.getItem(attribut.getAttributName(fuerFahrStreifen))
				.getScaledValue("beta2").doubleValue(); //$NON-NLS-1$
	}

	/**
	 * Erfragt den Startwert für die Glaettung
	 * 
	 * @return Startwert für die Glaettung
	 */
	public final long getStart() {
		return start;
	}

	/**
	 * Erfragt den Glaettungsparameter für abnehmende Messwerte
	 * 
	 * @return Glaettungsparameter für abnehmende Messwerte
	 */
	public final double getAlpha1() {
		return alpha1;
	}

	/**
	 * Erfragt den Glaettungsparameter für steigende Messwerte
	 * 
	 * @return Glaettungsparameter für steigende Messwerte
	 */
	public final double getAlpha2() {
		return alpha2;
	}

	/**
	 * Erfragt den Prognoseparameter für abnehmende Messwerte
	 * 
	 * @return Prognoseparameter für abnehmende Messwerte
	 */
	public final double getBeta1() {
		return beta1;
	}

	/**
	 * Erfragt den Prognoseparameter für steigende Messwerte
	 * 
	 * @return Prognoseparameter für steigende Messwerte
	 */
	public final double getBeta2() {
		return beta2;
	}

	/**
	 * Erfragt, ob dieses Objekt bereits mit Daten gefuellt wurde
	 * 
	 * @return ob dieses Objekt bereits mit Daten gefuellt wurde
	 */
	public final boolean isInitialisiert() {
		return this.start != -4;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		boolean gleich = false;

		if (obj != null && obj instanceof PrognoseAttributParameter) {
			PrognoseAttributParameter that = (PrognoseAttributParameter) obj;
			gleich = this.attribut.equals(that.attribut)
					&& this.start == that.start && this.alpha1 == that.alpha1
					&& this.alpha2 == that.alpha2 && this.beta1 == that.beta1
					&& this.beta2 == that.beta2;
		}

		return gleich;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "Attribut: " + this.attribut + " --> " + //$NON-NLS-1$ //$NON-NLS-2$
				", Start=" + DUAUtensilien.getTextZuMesswert(this.start) + //$NON-NLS-1$
				", alpha1=" + this.alpha1 + //$NON-NLS-1$
				", alpha2=" + this.alpha2 + //$NON-NLS-1$
				", beta1=" + this.beta1 + //$NON-NLS-1$
				", beta2=" + this.beta2; //$NON-NLS-1$
	}
}
