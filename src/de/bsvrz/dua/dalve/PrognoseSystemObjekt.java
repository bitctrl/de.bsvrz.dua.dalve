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
package de.bsvrz.dua.dalve;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.configuration.interfaces.AttributeGroup;
import stauma.dav.configuration.interfaces.SystemObject;
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
public class PrognoseSystemObjekt {

	/**
	 * zeigt an, ob dieses Objekt vom Typ <code>typ.fahrStreifen</code> ist
	 */
	private boolean objektIstFahrStreifen = false;
	
	/**
	 * das Systemobjekt selbst
	 */
	private SystemObject objekt = null;
	
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
	private AttributeGroup quellAtg = null;
	
	
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
		if(objekt == null){
			throw new NullPointerException("Uebergebenes Prognoseobjekt ist <<null>>"); //$NON-NLS-1$
		}
		if(objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)){
			this.objektIstFahrStreifen = true;
			this.pubAtgPrognose = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_TRENT_FS);
			this.pubAtgGlatt = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_FS);
			this.quellAtg = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS);
		}else
		if(objekt.isOfType(DUAKonstanten.TYP_MQ)){
			this.objektIstFahrStreifen = false;
			this.pubAtgPrognose = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_TRENT_MQ);
			this.pubAtgGlatt = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_MQ);
			this.quellAtg = dav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ);
		}else{
			throw new DUAInitialisierungsException("Uebergebenes Prognoseobjekt ist" + //$NON-NLS-1$
					" weder Fahrstreifen noch Messquerschnitt"); //$NON-NLS-1$
		}
	}
	
	
	/**
	 * Erfragt das Systemobjekt selbst
	 * 
	 * @return das Systemobjekt selbst
	 */
	public final SystemObject getObjekt(){
		return this.objekt;
	}
	
	
	/**
	 * Erfragt, ob dieses Objekt vom Typ <code>typ.fahrStreifen</code> ist
	 * 
	 * @return ob dieses Objekt vom Typ <code>typ.fahrStreifen</code> ist
	 */
	public final boolean isFahrStreifen(){
		return this.objektIstFahrStreifen;
	}
	
	
	/**
	 * Erfragt die Attributgruppe aus der sich die Prognosedaten dieses Objektes speisen
	 * 
	 * @return Attributgruppe aus der sich die Prognosedaten dieses Objektes speisen
	 */
	public final AttributeGroup getQuellAtg(){
		return this.quellAtg;
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
