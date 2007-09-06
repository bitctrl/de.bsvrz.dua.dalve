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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DaSystemObjekt;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;

/**
 * Objekt, das mit einem Systemobjekt assoziiert ist, welches innerhalb der 
 * Messwertprognose behandelt werden kann (also <code>typ.fahrStreifen</code>
 * oder <code>typ.messQuerschnitt</code>)
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class PrognoseSystemObjekt
extends DaSystemObjekt{
	
	/**
	 * Attributgruppe unter der die geglaetteten Werte publiziert werden
	 */
	private AttributeGroup pubAtgGlatt = null;
	
	/**
	 * Attributgruppe unter der die Prognosewerte publiziert werden
	 */
	private AttributeGroup pubAtgPrognose = null;
	
	/**
	 * Attributgruppe aus der sich die Prognosedaten dieses Objektes speisen
	 */
	private AttributeGroup analyseAtg = null;
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param objekt ein Systemobjekt, welches innerhalb der Messwertprognose
	 * behandelt werden kann (also <code>typ.fahrStreifen</code> oder
	 * <code>typ.messQuerschnitt</code>)
	 * @throws DUAInitialisierungsException
	 */
	public PrognoseSystemObjekt(final ClientDavInterface dav,
					 	  		final SystemObject objekt)
	throws DUAInitialisierungsException{
		super(dav, objekt);
		if(this.isFahrStreifen()){
			this.pubAtgPrognose = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_TRENT_FS);
			this.pubAtgGlatt = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_FS);
			this.analyseAtg = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS);
		}else{
			this.pubAtgPrognose = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_TRENT_MQ);
			this.pubAtgGlatt = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_MQ);
			this.analyseAtg = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ);
		}
	}
	
	
	/**
	 * Erfragt die Attributgruppe in der die Analysedaten dieses Objektes stehen
	 * 
	 * @return Attributgruppe in der die Analysedaten dieses Objektes stehen
	 */
	public final AttributeGroup getAnalyseAtg(){
		return this.analyseAtg;
	}
	
	
	/**
	 * Erfragt die Attributgruppe unter der die geglaetteten Werte publiziert werden
	 * 
	 * @return Attributgruppe unter der die geglaetteten Werte publiziert werden
	 */
	public final AttributeGroup getPubAtgGlatt(){
		return this.pubAtgGlatt;
	}

	
	/**
	 * Erfragt die Attributgruppe unter der die Prognosewerte publiziert werden
	 * 
	 * @return Attributgruppe unter der die Prognosewerte publiziert werden
	 */
	public final AttributeGroup getPubAtgPrognose(){
		return this.pubAtgPrognose;
	}
}
