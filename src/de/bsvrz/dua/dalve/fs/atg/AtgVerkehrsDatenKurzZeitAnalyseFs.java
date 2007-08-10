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
package de.bsvrz.dua.dalve.fs.atg;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.dua.AllgemeinerDatenContainer;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Korrespondiert mit der Attributgruppe <code>atg.verkehrsDatenKurzZeitAnalyseFs</code>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AtgVerkehrsDatenKurzZeitAnalyseFs 
extends AllgemeinerDatenContainer
implements ClientReceiverInterface{

	/**
	 * <code>kKfz.Grenz<code>
	 */
	private long kKfzGrenz = -4;

	/**
	 * <code>kKfz.Max<code>
	 */
	private long kKfzMax = -4;
	
	/**
	 * <code>kLkw.Grenz<code>
	 */
	private long kLkwGrenz = -4;

	/**
	 * <code>kLkw.Max<code>
	 */
	private long kLkwMax = -4;
	
	/**
	 * <code>kPkw.Grenz<code>
	 */
	private long kPkwGrenz = -4;
	
	/**
	 * <code>kPkw.Max<code>
	 */
	private long kPkwMax = -4;
	
	/**
	 * <code>kB.Grenz<code>
	 */
	private long kBGrenz = -4;
	
	/**
	 * <code>kB.Max<code>
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
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteiler-Verbindung
	 * @param fs ein Systemobjekt eines Fahrstreifens
	 */
	public AtgVerkehrsDatenKurzZeitAnalyseFs(final ClientDavInterface dav, 
											 final SystemObject fs){
		if(dav == null){
			throw new NullPointerException("Datenverteiler-Verbindung ist <<null>>"); //$NON-NLS-1$
		}
		if(fs == null){
			throw new NullPointerException("Uebergebenes Systemobjekt ist <<null>>"); //$NON-NLS-1$
		}
		dav.subscribeReceiver(this, fs, new DataDescription(
				dav.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitAnalyseFs"), //$NON-NLS-1$
				dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL),
				(short)0), ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	
	/**
	 * Erfragt <code>kKfz.Grenz<code>
	 * 
	 * @return <code>kKfz.Grenz<code>
	 */
	public final long getKKfzGrenz() {
		return kKfzGrenz;
	}


	/**
	 * Erfragt <code>kKfz.Max<code>
	 * 
	 * @return <code>kKfz.Max<code>
	 */
	public final long getKKfzMax() {
		return kKfzMax;
	}


	/**
	 * Erfragt <code>kLkw.Grenz<code>
	 * 
	 * @return <code>kLkw.Grenz<code>
	 */
	public final long getKLkwGrenz() {
		return kLkwGrenz;
	}


	/**
	 * Erfragt <code>kLkw.Max<code>
	 * 
	 * @return <code>kLkw.Max<code>
	 */
	public final long getKLkwMax() {
		return kLkwMax;
	}


	/**
	 * Erfragt <code>kPkw.Grenz<code>
	 * 
	 * @return <code>kPkw.Grenz<code>
	 */
	public final long getKPkwGrenz() {
		return kPkwGrenz;
	}


	/**
	 * Erfragt <code>kPkw.Max<code>
	 * 
	 * @return <code>kPkw.Max<code>
	 */
	public final long getKPkwMax() {
		return kPkwMax;
	}


	/**
	 * Erfragt <code>kB.Grenz<code>
	 * 
	 * @return <code>kB.Grenz<code>
	 */
	public final long getKBGrenz() {
		return kBGrenz;
	}


	/**
	 * Erfragt <code>kB.Max<code>
	 * 
	 * @return <code>kB.Max<code>
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
						
						this.kBGrenz = resultat.getData().getUnscaledValue("kB.Grenz").longValue(); //$NON-NLS-1$
						this.kBMax = resultat.getData().getUnscaledValue("kB.Max").longValue(); //$NON-NLS-1$
						
						this.kKfzGrenz = resultat.getData().getUnscaledValue("kKfz.Grenz").longValue(); //$NON-NLS-1$
						this.kKfzMax = resultat.getData().getUnscaledValue("kKfz.Max").longValue(); //$NON-NLS-1$
						
						this.kLkwGrenz = resultat.getData().getUnscaledValue("kLkw.Grenz").longValue(); //$NON-NLS-1$
						this.kLkwMax = resultat.getData().getUnscaledValue("kLkw.Max").longValue(); //$NON-NLS-1$
						
						this.kPkwGrenz = resultat.getData().getUnscaledValue("kPkw.Grenz").longValue(); //$NON-NLS-1$
						this.kPkwMax = resultat.getData().getUnscaledValue("kPkw.Max").longValue(); //$NON-NLS-1$						
					}
				}
			}
		}
	}

}
