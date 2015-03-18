/*
 * Segment 4 Daten�bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007-2015 BitCtrl Systems GmbH
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
 * Wei�enfelser Stra�e 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */

/**
 * Klassen fuer die Ermittlung der geglaetteten Werte, sowie der
 * Prognosewerte auf Basis der LVE-Analysewerte fuer Fahrstreifen
 * und Messquerschnitte<br>
 * Attributgruppen:<br>
 * <code>atg.verkehrsDatenKurzZeitGegl�ttetFs</code><br>
 * <code>atg.verkehrsDatenKurzZeitTrendExtraPolationFs</code><br>
 * <code>atg.verkehrsDatenKurzZeitGegl�ttetMq</code><br>
 * <code>atg.verkehrsDatenKurzZeitTrendExtraPolationMq</code><br>
 * Aspekte:<br>
 * <code>asp.prognoseFlink</code><br>
 * <code>asp.prognoseNormal</code><br>
 * <code>asp.prognoseTr�ge</code><br>
 */

package de.bsvrz.dua.dalve.prognose;

