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

import junit.framework.Assert;

import org.junit.Test;

import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;

/**
 * Testet die Implementierungs des Algorithmus zur Messwertprognose
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AbstraktAttributPrognoseObjektTest {

	/**
	 * Algorithmus zur Messwertprognose
	 */
	AbstraktAttributPrognoseObjekt prognose = new AbstraktAttributPrognoseObjekt();
		

	/**
	 * Testet die Implementierungs des Algorithmus zur Messwertprognose
	 */
	@Test
	public final void test1(){

		/**
		 * Z <= ZAlt (starke Glaettung)
		 */
		prognose.alpha1 = 0.05;
		prognose.beta1 = 0.05;

		/**
		 * Z > ZAlt (schwache Glaettung)
		 */
		prognose.alpha2 = 0.95;
		prognose.beta2 = 0.95;
		
		prognose.ZAlt = 9000;

		long[][] schwachSoll = new long[10][2];
		long[][] starkSoll = new long[10][2];
		
		/**
		 * Z > ZAlt (schwache Glaettung)
		 */
		int c = 0;
		for(long i=10000; i<20000; i+=1000){
			try {
				prognose.berechneGlaettungsParameterUndStart(i, false);
				System.out.println("N = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$
				
				schwachSoll[c][0] = Math.abs(i - prognose.getZP());
				schwachSoll[c][1] = Math.abs(i - prognose.getZG());
				
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
			c++;
		}

		/**
		 * Z <= ZAlt (starke Glaettung)
		 */
		c = 0;
		for(long i=20000; i>10000; i-=1000){
			try {
				prognose.berechneGlaettungsParameterUndStart(i, false);
				System.out.println("N = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$
				
				starkSoll[c][0] = Math.abs(i - prognose.getZP());
				starkSoll[c][1] = Math.abs(i - prognose.getZG());
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
			c++;
		}
		
		for(c = 0; c<10; c++){
			Assert.assertTrue("ZP, N = " + c, schwachSoll[c][0] <= starkSoll[c][0]); //$NON-NLS-1$
			Assert.assertTrue("ZG, N = " + c, schwachSoll[c][1] <= starkSoll[c][1]); //$NON-NLS-1$
		}
		
		for(long i=10000; i>0; i-=1000){
			try {
				prognose.berechneGlaettungsParameterUndStart(i, false);
				System.out.println("N1 = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$				
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
		}
		
		for(long i=0; i<1000; i++){
			try {
				prognose.berechneGlaettungsParameterUndStart(0, false);
				System.out.println("N2 = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$				
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
		}		

		
		/**
		 * Z <= ZAlt (starke Glaettung)
		 */
		prognose.alpha1 = 0.05;
		prognose.beta1 = 0.05;

		/**
		 * Z > ZAlt (schwache Glaettung)
		 */
		prognose.alpha2 = 0.95;
		prognose.beta2 = 0.95;
		
		prognose.ZAlt = 9000;
		
		/**
		 * Z > ZAlt (schwache Glaettung)
		 */
		for(long i=10000; i<20000; i+=1000){
			try {
				prognose.berechneGlaettungsParameterUndStart(i, true);
				System.out.println("N = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$				
				
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Z <= ZAlt (starke Glaettung)
		 */
		for(long i=20000; i>10000; i-=1000){
			try {
				prognose.berechneGlaettungsParameterUndStart(i, true);
				System.out.println("N = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
		}
		
		
		for(long i=10000; i>0; i-=1000){
			try {
				prognose.berechneGlaettungsParameterUndStart(i, true);
				System.out.println("N1 = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$				
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
		}
		
		for(long i=0; i<1000; i++){
			try {
				prognose.berechneGlaettungsParameterUndStart(0, true);
				System.out.println("N2 = " + c + ", Z = " + i + ", ZP = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						prognose.getZP() + ", ZG = " + prognose.getZG()); //$NON-NLS-1$				
			} catch (PrognoseParameterException e) {
				e.printStackTrace();
			}
		}
	}	
}
