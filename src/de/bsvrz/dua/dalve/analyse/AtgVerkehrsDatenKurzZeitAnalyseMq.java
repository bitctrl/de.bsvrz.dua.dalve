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

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.Data.Array;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.dua.AllgemeinerDatenContainer;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Korrespondiert mit der Attributgruppe <code>atg.verkehrsDatenKurzZeitAnalyseMq</code>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AtgVerkehrsDatenKurzZeitAnalyseMq 
extends AllgemeinerDatenContainer
implements ClientReceiverInterface{
	
	/**
	 * <code>KKfz.Grenz<code>
	 */
	private long kKfzGrenz = -4;

	/**
	 * <code>KKfz.Max<code>
	 */
	private long kKfzMax = -4;
	
	/**
	 * <code>KLkw.Grenz<code>
	 */
	private long kLkwGrenz = -4;

	/**
	 * <code>KLkw.Max<code>
	 */
	private long kLkwMax = -4;
	
	/**
	 * <code>KPkw.Grenz<code>
	 */
	private long kPkwGrenz = -4;
	
	/**
	 * <code>KPkw.Max<code>
	 */
	private long kPkwMax = -4;
	
	/**
	 * <code>KB.Grenz<code>
	 */
	private long kBGrenz = -4;
	
	/**
	 * <code>KB.Max<code>
	 */
	private long kBMax = -4;
	
	/**
	 * <code>fl.k1<code>
	 */
	private double flk1 = -4;
	
	/**
	 * <code>fl.k2<code>
	 */
	private double flk2 = -4;
	
	/**
	 * Wichtung zur Ermittlung der Differenzgeschwindigkeit im Messquerschnitt 
	 */
	private int[] wichtung = null; 
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteiler-Verbindung
	 * @param mq ein Systemobjekt eines Messquerschnittes
	 */
	public AtgVerkehrsDatenKurzZeitAnalyseMq(ClientDavInterface dav, 
											 SystemObject mq) {
		if(dav == null){
			throw new NullPointerException("Datenverteiler-Verbindung ist <<null>>"); //$NON-NLS-1$
		}
		if(mq == null){
			throw new NullPointerException("Uebergebenes Systemobjekt ist <<null>>"); //$NON-NLS-1$
		}
		dav.subscribeReceiver(this, mq, new DataDescription(
				dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitAnalyseMq"), //$NON-NLS-1$
				dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL),
				(short)0), ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	
	/**
	 * Erfragt <code>KKfz.Grenz<code>
	 * 
	 * @return <code>KKfz.Grenz<code>
	 */
	public final long getKKfzGrenz() {
		return kKfzGrenz;
	}


	/**
	 * Erfragt <code>KKfz.Max<code>
	 * 
	 * @return <code>KKfz.Max<code>
	 */
	public final long getKKfzMax() {
		return kKfzMax;
	}


	/**
	 * Erfragt <code>KLkw.Grenz<code>
	 * 
	 * @return <code>KLkw.Grenz<code>
	 */
	public final long getKLkwGrenz() {
		return kLkwGrenz;
	}


	/**
	 * Erfragt <code>KLkw.Max<code>
	 * 
	 * @return <code>KLkw.Max<code>
	 */
	public final long getKLkwMax() {
		return kLkwMax;
	}


	/**
	 * Erfragt <code>KPkw.Grenz<code>
	 * 
	 * @return <code>KPkw.Grenz<code>
	 */
	public final long getKPkwGrenz() {
		return kPkwGrenz;
	}


	/**
	 * Erfragt <code>KPkw.Max<code>
	 * 
	 * @return <code>KPkw.Max<code>
	 */
	public final long getKPkwMax() {
		return kPkwMax;
	}


	/**
	 * Erfragt <code>KB.Grenz<code>
	 * 
	 * @return <code>KB.Grenz<code>
	 */
	public final long getKBGrenz() {
		return kBGrenz;
	}


	/**
	 * Erfragt <code>KB.Max<code>
	 * 
	 * @return <code>KB.Max<code>
	 */
	public final long getKBMax() {
		return kBMax;
	}


	/**
	 * Erfragt <code>fl.k1<code>
	 * 
	 * @return <code>fl.k1<code>
	 */
	public final double getFlk1() {
		return flk1;
	}


	/**
	 * Erfragt <code>fl.k2<code>
	 * 
	 * @return <code>fl.k2<code>
	 */
	public final double getFlk2() {
		return flk2;
	}
	
	
	/**
	 * Erfragt die Gewichtungsfaktoren.<br> 
	 * Der Gewichtungsfaktor w(j) wird bei der Ermittlung der gewichteten Differenzgeschwindigkeit
	 * VDelta(i) im Messquerschnitt i benötigt ref.Afo . Dabei wichtet der Faktor w(1) die
	 * Differenzgeschwindigkeit zwischen dem Hauptfahrstreifen und dem 1. Überholfahrstreifen,
	 * w(2) die Differenzgeschwindigkeit zwischen dem 1. ÜFS und dem 2.ÜFS usw.. Die Summe der
	 * Gewichtungsfaktoren muss eins sein. <br>
	 * Über den Parameter wichtung wird der Gewichtungsfaktor ermittelt. 
	 * Wenn das Array keine Elemente enthält wird gleich gewichtet. D.h. die Differenzgeschwindigkeiten
	 * zwischen zwei benachbarten Fahrstreifen gehen zu gleichen Teilen in die Ermittlung von VDelta ein.<br>
	 * Hinweis: Die Wichtung kann erst ab mindestens drei Fahrstreifen gesetzt werden. 
	 * Wenn das Array Elemente enthält werden die Werte für die Wichtung der Differenzgeschwindigkeiten
	 * zwischen den benachbarten Fahrstreifen umgerechnet: <br>
	 * Wenn zu wenig Werte vorgegeben sind, werden die weiteren benötigten Werte durch duplizieren des letzten Wertes erzeugt. 
	 * Für einen Messquerschnitt mit 4 Fahrstreifen werden z.B. drei Werte benötigt. Ist als wichtung (60,40)
	 * parametriert, wird für die Ermittlung der Gewichtungsfaktoren das Array (60,40,40) betrachtet. 
	 * Wenn die Summe der Werte 100 Prozent überschreitet oder unterschreitet werden die Gewichtungswerte
	 * auf 100 Prozent normiert. Damit ergeben sich aus dem obigen Beispiel folgende Wichtungswerte:
	 * w(1)=60/140, w(2)=w(3)=40/140. 
	 * 
	 * @return die Gewichtungsfaktoren
	 */
	public final int[] getWichtung(){
		return wichtung;
	}


	/**
	 * Erfragt, ob dieses Objekt bereits Parameter emfangen hat
	 * 
	 * @return ob dieses Objekt bereits Parameter emfangen hat
	 */
	public final boolean isInitialisiert(){
		return this.flk1 != -4;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null && resultat.getData() != null){
					synchronized (this) {
						this.flk1 = resultat.getData().getScaledValue("fl.k1").doubleValue(); //$NON-NLS-1$
						this.flk2 = resultat.getData().getScaledValue("fl.k2").doubleValue(); //$NON-NLS-1$
						
						this.kBGrenz = resultat.getData().getUnscaledValue("KB.Grenz").longValue(); //$NON-NLS-1$
						this.kBMax = resultat.getData().getUnscaledValue("KB.Max").longValue(); //$NON-NLS-1$
						
						this.kKfzGrenz = resultat.getData().getUnscaledValue("KKfz.Grenz").longValue(); //$NON-NLS-1$
						this.kKfzMax = resultat.getData().getUnscaledValue("KKfz.Max").longValue(); //$NON-NLS-1$
						
						this.kLkwGrenz = resultat.getData().getUnscaledValue("KLkw.Grenz").longValue(); //$NON-NLS-1$
						this.kLkwMax = resultat.getData().getUnscaledValue("KLkw.Max").longValue(); //$NON-NLS-1$
						
						this.kPkwGrenz = resultat.getData().getUnscaledValue("KPkw.Grenz").longValue(); //$NON-NLS-1$
						this.kPkwMax = resultat.getData().getUnscaledValue("KPkw.Max").longValue(); //$NON-NLS-1$
						
						Array array = resultat.getData().getArray("wichtung"); //$NON-NLS-1$
						if(array.getLength() > 0){
							this.wichtung = new int[array.getLength()];
						}
						
						for(int i=0; i<array.getLength(); i++){
							this.wichtung[i] = array.getItem(i).asUnscaledValue().intValue();
						}
					}
				}
			}
		}
	}

}
