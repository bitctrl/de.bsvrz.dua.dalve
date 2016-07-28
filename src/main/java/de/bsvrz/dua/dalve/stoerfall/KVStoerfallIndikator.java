/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
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

package de.bsvrz.dua.dalve.stoerfall;

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.dalve.prognose.PrognoseTyp;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Basisklasse für einen Störfallindikator, der den Störfalll anhand der Werte K (Dichte) und V( Geschwindigkeit) ermittelt.
 * 
 * Wird von den Störfallindikatoren MARZ, NRW und RDS verwendet. K berechnet sich aus Q / V mit q = qKfzGNormal und v berechnet sich entweder aus dem vgKfz-Wert
 * entsprechend TLS (bei Messquerschnitten dem Minimum der einzenen Fahrstreifen) oder (falls dieser Wert nicht verfügbar ist) aus vKfz der Datenaufbereitung.
 *
 * @author Kappich Systemberatung
 */
public abstract class KVStoerfallIndikator extends AbstraktStoerfallIndikator {
	/**
	 * Grenzgeschwindigkeit 1 (0 &lt; v1 &lt; v2)
	 */
	protected long v1 = -4;
	/**
	 * Grenzgeschwindigkeit 2 (0 &lt; v1 &lt; v2)
	 */
	protected long v2 = -4;
	/**
	 * Grenzfahrzeugdichte 1 (0 &lt; k1 &lt; k2 &lt; k3)
	 */
	protected long k1 = -4;
	/**
	 * Grenzfahrzeugdichte 2 (0 &lt; k1 &lt; k2 &lt; k3)
	 */
	protected long k2 = -4;
	/**
	 * Grenzfahrzeugdichte 3 (0 &lt; k1 &lt; k2 &lt; k3)
	 */
	protected long k3 = -4;
	/**
	 * Grenzfahrzeugdichte 5T (0 &lt; k5T &lt; k3)
	 */
	protected long kT = -4;

	/**
	 * Aktuelle Daten der einzelnen Fahrstreifen
	 */
	private final Map<SystemObject, ResultData> _fsData = new HashMap<>();

	/**
	 * Aktueller Prognosedatensatz zur Bestimmung von Q
	 */
	private ResultData _prognoseData = null;

	/**
	 * Aktueller Analysedatensatz zur Bestimmung von V (bei MQ werden die einzelnen FS-Werte bevorzugt verwendet)
	 */
	protected ResultData _analyseData = null;

	@Override
	public void initialisiere(ClientDavInterface dav, SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);
		
		if(objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			if(objekt.isOfType(DUAKonstanten.TYP_MQ)) {
				ConfigurationObject configurationObject = (ConfigurationObject) objekt;
				final List<SystemObject> fahrStreifen = configurationObject.getObjectSet("FahrStreifen").getElements();
				dav.subscribeReceiver(this, fahrStreifen, new DataDescription(
						DatenaufbereitungLVE.analyseAtgFS,
						dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)
				), ReceiveOptions
						                      .normal(), ReceiverRole.receiver());
			}
		}
			
		/**
		 * Anmeldung auf Daten
		 */
		dav.subscribeReceiver(this, objekt, new DataDescription(
				DatenaufbereitungLVE.getPubAtgGlatt(this.objekt),
				PrognoseTyp.NORMAL.getAspekt()), ReceiveOptions
				.normal(), ReceiverRole.receiver());	
		
		dav.subscribeReceiver(this, objekt, new DataDescription(
				DatenaufbereitungLVE.getAnalyseAtg(this.objekt),
				dav.getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE)), ReceiveOptions
				.normal(), ReceiverRole.receiver());
		
	}

	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten
	 * AFo DuA 6.6.4.2.1.1
	 * 
	 * @param resultat
	 *            ein empfangenes geglaettes Datum mit Nutzdaten
	 */
	protected void berechneStoerfallIndikator(ResultData resultat) {
		if(resultat.getObject().isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) && objekt.isOfType(DUAKonstanten.TYP_MQ)) {
			_fsData.put(resultat.getObject(), resultat);
		}
		else if(Objects.equals(resultat.getDataDescription().getAspect(), PrognoseTyp.NORMAL.getAspekt())){
			_prognoseData = resultat;
		}
		else {
			_analyseData = resultat;
		}

		ValueAndTimestamp v = getV();
		ValueAndTimestamp q = getQ();
		
		if(v != null && q != null && v.getTimestamp() == q.getTimestamp() && resultat.getDataTime() == v.getTimestamp() && _analyseData != null && _analyseData.getData() != null) {
			berechneIndikator(v.getTimestamp(), v.getValue(), q.getValue());
		}
	}

	/**
	 * Berechnet den aktuellen Störfall (abstakte Methode) aus v und q.
	 * @param timeStamp Zeitstempel der übergebenen Daten. Die Methode wird nur mit zusammenpassenden Daten aufgerufen.
	 * @param v Aktueller V-Wert in km/h (falls &lt; 0 handelt es sich um einen (Fehler-)Zustand entsprechend {@link DUAKonstanten}).
	 * @param q  Aktueller Q-Wert in Fz-E/h (falls &lt; 0 handelt es sich um einen (Fehler-)Zustand entsprechend {@link DUAKonstanten}).
	 */
	protected abstract void berechneIndikator(long timeStamp, long v, long q);

	/** 
	 * Gibt den aktuellen V-Wert zurück
	 * @return den aktuellen V-Wert oder null falls nicht verfügbar oder einen Wert &lt; 0 falls der Wert nicht ermittelbar/fehlerhaft ist.
	 */
	private ValueAndTimestamp getV() {
		if(this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
			if(_analyseData == null) return null;
			if(!_analyseData.hasData()) return new ValueAndTimestamp(-4, _analyseData.getDataTime());
			long value = _analyseData.getData().getItem("vgKfz").getUnscaledValue("Wert").longValue();
			if(value >= 0){
				return new ValueAndTimestamp(value, _analyseData.getDataTime());
			}
			return new ValueAndTimestamp(_analyseData.getData().getItem("vKfz").getUnscaledValue("Wert").longValue(), _analyseData.getDataTime());
		}
		else {
			if(this.objekt.isOfType(DUAKonstanten.TYP_MQ)) {
				ValueAndTimestamp value = getMinVKfzGFs();
				if(value == null) return null;
				if(value.getValue() >= 0) {
					return value;
				}
			}
			if(_analyseData == null) return null;
			if(!_analyseData.hasData()) return new ValueAndTimestamp(-4, _analyseData.getDataTime());
			return new ValueAndTimestamp(_analyseData.getData().getItem("VKfz").getUnscaledValue("Wert").longValue(), _analyseData.getDataTime());
		}

	}

	/**
	 * Berechnet den Minimum-Wert der einzelnen Fahrstreifenwerte (falls alle FS bereits aktuele Daten empfangen haben)
	 * @return Minimum der FS-Werte oder null, falls noch nicht für alle FS zusammenpassende, gültige Daten eingetroffen sind.
	 */
	private ValueAndTimestamp getMinVKfzGFs() {
		long timestamp = -1;
		long minValue = Long.MAX_VALUE;
		for(ResultData data : _fsData.values()) {
			if(timestamp != -1 && data.getDataTime() != timestamp) return null;
			timestamp = data.getDataTime();
			if(data.hasData()) {
				minValue = Math.min(minValue, data.getData().getItem("vgKfz").getUnscaledValue("Wert").longValue());
			}
			else {
				minValue = -4;
			}
		}
		if(minValue == Long.MAX_VALUE) return null;
		return new ValueAndTimestamp(minValue, timestamp);
	}


	/**
	 * Berechnet den Minimum-Wert der einzelnen Fahrstreifenwerte (falls alle FS bereits aktuele Daten empfangen haben)
	 * @return Minimum der FS-Werte oder null, falls noch nicht für alle FS zusammenpassende, gültige Daten eingetroffen sind.
	 */
	public long getT() {
		if(this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)){
			return _analyseData.getData().getTimeValue("T").getMillis();
		}
		else {
			for(ResultData data : _fsData.values()) {
				if(data.hasData()) {
					return data.getData().getTimeValue("T").getMillis();
				}
			}
			return 0;
		}
	}
	
	/**
	 * Gibt den aktuellen Q-Wert zurück
	 * @return den aktuellen Q-Wert oder null falls nicht verfügbar oder einen Wert &lt; 0 falls der Wert nicht ermittelbar/fehlerhaft ist.
	 */
	private ValueAndTimestamp getQ() {
		if(_prognoseData == null) return null;
		if(!_prognoseData.hasData()) return new ValueAndTimestamp(-4, _prognoseData.getDataTime());
		String attrK = this.objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN) ? "qKfzG" : "QKfzG";
		return new ValueAndTimestamp(_prognoseData.getData().getItem(attrK).getUnscaledValue("Wert").longValue(), _prognoseData.getDataTime());
	}

	@Override
	protected void readParameter(ResultData parameter) {
		if (parameter.getData() != null) {
			this.v1 = parameter.getData().getUnscaledValue("v1").longValue(); //$NON-NLS-1$
			this.v2 = parameter.getData().getUnscaledValue("v2").longValue(); //$NON-NLS-1$
			this.k1 = parameter.getData().getUnscaledValue("k1").longValue(); //$NON-NLS-1$
			this.k2 = parameter.getData().getUnscaledValue("k2").longValue(); //$NON-NLS-1$
			this.k3 = parameter.getData().getUnscaledValue("k3").longValue(); //$NON-NLS-1$
			this.kT = parameter.getData().getUnscaledValue("kT").longValue(); //$NON-NLS-1$

			/**
			 * Konsistenz-Check
			 */
			if (!(v1 > 0 && v1 < v2)) {
				Debug.getLogger().warning(
						"Fehlerhafte Parameter (0 < v1 < v2) empfangen fuer " + //$NON-NLS-1$
								this.objekt + ": v1 = " + v1 + ", v2 = " + v2); //$NON-NLS-1$//$NON-NLS-2$
			}
			if (!(k1 > 0 && k1 < k2 && k2 < k3)) {
				Debug.getLogger().warning(
						"Fehlerhafte Parameter (0 < k1 < k2 < k3) empfangen fuer " + //$NON-NLS-1$
								this.objekt + ": k1 = " + k1 + ", k2 = " + k2 + ", k3 = " + k3); //$NON-NLS-1$//$NON-NLS-2$
			}
			if (!(kT > 0 && kT < k3)) {
				Debug.getLogger().warning(
						"Fehlerhafte Parameter (0 < kT < k3) empfangen fuer " + //$NON-NLS-1$
								this.objekt + ": kT = " + kT + ", k3 = " + k3); //$NON-NLS-1$//$NON-NLS-2$
			}
		} else {
			this.v1 = -4;
			this.v2 = -4;
			this.k1 = -4;
			this.k2 = -4;
			this.k3 = -4;
			this.kT = -4;
		}
	}

	private class ValueAndTimestamp {
		
		private final long _value;
		
		private final long _timestamp;

		public ValueAndTimestamp(final long value, final long timestamp) {
			_timestamp = timestamp;
			_value = value;
		}

		public long getValue() {
			return _value;
		}

		public long getTimestamp() {
			return _timestamp;
		}

		@Override
		public String toString() {
			return "ValueAndTimestamp{" +
					"_value=" + _value +
					", _timestamp=" + _timestamp +
					'}';
		}
	}
}
