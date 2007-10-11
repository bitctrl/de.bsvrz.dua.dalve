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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;

/**
 * Objekt, das mit einem Systemobjekt assoziiert ist, welches innerhalb der 
 * Datenaufbereitung behandelt werden kann (also <code>typ.fahrStreifen</code>
 * oder <code>typ.messQuerschnitt</code>).
 *  
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DaSystemObjekt {

	/**
	 * zeigt an, ob dieses Objekt vom Typ <code>typ.fahrStreifen</code> ist
	 */
	protected boolean objektIstFahrStreifen = false;
	
	/**
	 * das Systemobjekt selbst
	 */
	protected SystemObject objekt = null;
	
		
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param objekt ein Systemobjekt, welches innerhalb der Datenaufbereitung
	 * behandelt werden kann (also <code>typ.fahrStreifen</code> oder
	 * <code>typ.messQuerschnitt</code>)
	 * @throws DUAInitialisierungsException wenn das Objekt nicht identifizierbar ist
	 */
	public DaSystemObjekt(final ClientDavInterface dav,
					 	  final SystemObject objekt)
	throws DUAInitialisierungsException{
		if(objekt == null){
			throw new NullPointerException("Uebergebenes Objekt ist <<null>>"); //$NON-NLS-1$
		}
		this.objekt = objekt;
		if(objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)){
			this.objektIstFahrStreifen = true;
		}else
		if(objekt.isOfType(DUAKonstanten.TYP_MQ)){
			this.objektIstFahrStreifen = false;
		}else{
			throw new DUAInitialisierungsException("Uebergebenes Objekt ist" + //$NON-NLS-1$
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
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.objekt.toString();
	}
}
