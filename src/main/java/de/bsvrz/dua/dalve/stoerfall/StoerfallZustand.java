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
package de.bsvrz.dua.dalve.stoerfall;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.GanzZahl;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;

/**
 * Korrespondiert mit einem Datensatz der Attributgruppe
 * <code>atg.störfallZustand</code>
 *  
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class StoerfallZustand{
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	private ClientDavInterface DAV = null;
	
	/**
	 * Verkehrssituation (Level Of Service)
	 */
	private StoerfallSituation situation = null;
	
	/**
	 * Prognosehorizont (0 entspricht Analysewert)
	 */
	private long horizont = 0;
	
	/**
	 * Güte des betrachteten Wertes
	 */
	private GanzZahl guete = GanzZahl.getGueteIndex();
	
	/**
	 * Berechnungsverfahren, mit dem die Güte ermittelt wurde
	 */
	private GueteVerfahren verfahren = GueteVerfahren.STANDARD;

	/**
	 * Intervalldauer, mit dem die Werte erfasst wurden
	 */
	private long T = 1;
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 */
	public StoerfallZustand(ClientDavInterface dav){
		if(DAV == null){
			DAV = dav;
		}
	}

	
	/**
	 * Erfragt den Inhalt dieses Objektes als einen DAV-Datensatz der Attributgruppe
	 * <code>atg.störfallZustand</code>
	 * 
	 * @return der Inhalt dieses Objektes als einen DAV-Datensatz der Attributgruppe
	 * <code>atg.störfallZustand</code>
	 */
	public final Data getData(){
		Data datenSatz = DAV.createData(DAV.getDataModel().getAttributeGroup("atg.störfallZustand")); //$NON-NLS-1$
		
		datenSatz.getTimeValue("T").setMillis(this.T); //$NON-NLS-1$
		datenSatz.getUnscaledValue("Situation").set(this.situation.getCode()); //$NON-NLS-1$
		datenSatz.getTimeValue("Horizont").setMillis(this.horizont); //$NON-NLS-1$
		datenSatz.getItem("Güte").getUnscaledValue("Index").set(this.guete.getWert()); //$NON-NLS-1$ //$NON-NLS-2$
		datenSatz.getItem("Güte").getUnscaledValue("Verfahren").set(this.verfahren.getCode()); //$NON-NLS-1$ //$NON-NLS-2$
		
		return datenSatz;
	}


	/**
	 * Setzt Verkehrssituation (Level Of Service)
	 * 
	 * @param situation Verkehrssituation (Level Of Service)
	 */
	public final void setSituation(StoerfallSituation situation) {
		this.situation = situation;
	}


	/**
	 * Setzt den Prognosehorizont (0 entspricht Analysewert)
	 * 
	 * @param horizont Prognosehorizont (0 entspricht Analysewert)
	 */
	public final void setHorizont(long horizont) {
		this.horizont = horizont;
	}


	/**
	 * Setzt die Güte des betrachteten Wertes
	 * 
	 * @param guete Güte des betrachteten Wertes
	 */
	public final void setGuete(GanzZahl guete) {
		this.guete = guete;
	}


	/**
	 * Setzt das Berechnungsverfahren, mit dem die Güte ermittelt wurde
	 * 
	 * @param verfahren Berechnungsverfahren, mit dem die Güte ermittelt wurde
	 */
	public final void setVerfahren(GueteVerfahren verfahren) {
		this.verfahren = verfahren;
	}

	
	/**
	 * Setzt die Intervalldauer, mit dem die Werte erfasst wurden
	 * 
	 * @param T Intervalldauer, mit dem die Werte erfasst wurden
	 */
	public final void setT(long T) {
		this.T = T;
	}
	
	
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public String toString() {
		return "T: " + this.T + //$NON-NLS-1$
			   "ms\nSituation: " + this.situation +  //$NON-NLS-1$
			   "\nHorizont: " + this.horizont +  //$NON-NLS-1$
			   "\nGuete: " + guete +  //$NON-NLS-1$
			   "\nVerfahren: " + this.verfahren; //$NON-NLS-1$
	}	

}
