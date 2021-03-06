/*******************************************************************************
 * Copyright (c) 2011 neXtep Software and contributors.
 * All rights reserved.
 *
 * This file is part of neXtep designer.
 *
 * NeXtep designer is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 *
 * NeXtep designer is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     neXtep Softwares - initial API and implementation
 *******************************************************************************/
package com.nextep.installer.model.impl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import com.nextep.installer.model.IDatabaseStructure;
import com.nextep.installer.model.IDatabaseStructureBuilder;

public class PostgreSqlStructureBuilder implements IDatabaseStructureBuilder {

	private Map<String, String> datatypeConversionMap = new HashMap<String, String>();

	public PostgreSqlStructureBuilder() {
		// FIXME Duplicate code with postgresqlSchemaCapturer for datatype custom conversion
		datatypeConversionMap.put("int8", "bigint"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("serial8", "bigserial"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("varbit", "bit varying"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("bool", "boolean"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("float8", "double precision"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("int4", "integer"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("int2", "smallint"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("int", "integer"); //$NON-NLS-1$ //$NON-NLS-2$
		datatypeConversionMap.put("serial4", "serial"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IDatabaseStructure buildStructure(String schema, Connection conn) throws SQLException {
		final IDatabaseStructure structure = new DatabaseStructure();

		Statement stmt = null;
		ResultSet rset = null;
		ResultSet rsetIdx = null;
		try {
			DatabaseMetaData md = conn.getMetaData();

			// Listing tables
			rset = md.getTables(null, schema, null, new String[] { "TABLE", "VIEW", "SEQUENCE" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			// Building an array of checked objects
			while (rset.next()) {
				DBObject dbObj = new DBObject(rset.getString("TABLE_TYPE"), //$NON-NLS-1$
						rset.getString("TABLE_NAME")); //$NON-NLS-1$
				structure.addObject(dbObj);
				try {
					rsetIdx = md.getIndexInfo(null, schema, dbObj.getName(), false, false);
					while (rsetIdx.next()) {
						structure.addObject(new DBObject(DBObject.TYPE_INDEX, rsetIdx
								.getString("INDEX_NAME"))); //$NON-NLS-1$
					}
				} catch (SQLException e) {
					// Unable to get the index, may we log anything?
				} finally {
					if (rsetIdx != null) {
						rsetIdx.close();
					}
				}
			}
			rset.close();

			// Fetching table columns
			rset = md.getColumns(null, schema, null, null);
			while (rset.next()) {
				final String tabName = rset.getString("TABLE_NAME"); //$NON-NLS-1$
				final String colName = rset.getString("COLUMN_NAME"); //$NON-NLS-1$
				structure.addObject(new DBObject("COLUMN", tabName + "." + colName)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			rset.close();

			// Fetching types Oid => type names mapping from Postgres
			final Map<String, String> typesOidMap = new HashMap<String, String>();
			stmt = conn.createStatement();
			rset = stmt.executeQuery("SELECT oid, typname FROM pg_type"); //$NON-NLS-1$
			while (rset.next()) {
				final String oid = rset.getString(1);
				final String typeName = rset.getString(2);
				typesOidMap.put(oid, convertType(typeName));
			}
			rset.close();
			stmt.close();

			// Fetching procedures
			stmt = conn.prepareStatement("SELECT " //$NON-NLS-1$
					+ "    p.proname AS procedure_name" //$NON-NLS-1$
					+ "  , p.proargtypes AS argument_types_oids " //$NON-NLS-1$
					+ "FROM pg_proc p " //$NON-NLS-1$
					+ "  LEFT JOIN pg_language l ON p.prolang=l.oid " //$NON-NLS-1$
					+ "  LEFT JOIN pg_namespace n ON p.pronamespace=l.oid " //$NON-NLS-1$
					+ "WHERE l.lanname!='internal' and (n.nspname=? or n.nspname is null)"); //$NON-NLS-1$
			((PreparedStatement) stmt).setString(1, schema);

			rset = ((PreparedStatement) stmt).executeQuery();
			while (rset.next()) {
				final String procName = rset.getString("procedure_name"); //$NON-NLS-1$
				final String types[] = rset.getString("argument_types_oids").split("(\\s)+"); //$NON-NLS-1$ //$NON-NLS-2$
				final StringBuffer args = new StringBuffer(50);
				args.append("("); //$NON-NLS-1$
				String separator = ""; //$NON-NLS-1$

				for (String t : types) {
					args.append(separator);
					if (typesOidMap.get(t) != null) {
						args.append(typesOidMap.get(t));
					}
					separator = ", "; //$NON-NLS-1$
				}
				args.append(")"); //$NON-NLS-1$
				structure.addObject(new DBObject("PROCEDURE", procName + args)); //$NON-NLS-1$
			}
			rset.close();
			stmt.close();

			stmt = conn.prepareStatement("SELECT " //$NON-NLS-1$
					+ "  trigger_name " //$NON-NLS-1$
					+ "FROM information_schema.triggers " //$NON-NLS-1$
					+ "WHERE trigger_schema NOT IN ('pg_catalog', 'information_schema')"); //$NON-NLS-1$
			rset = ((PreparedStatement) stmt).executeQuery();
			while (rset.next()) {
				final String triggerName = rset.getString("trigger_name"); //$NON-NLS-1$
				structure.addObject(new DBObject("TRIGGER", triggerName)); //$NON-NLS-1$
			}

			// We return the checked objects list
			return structure;
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (rsetIdx != null) {
				rsetIdx.close();
			}
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	/**
	 * Since PostgreSql make some weird datatype conversions we need to convert them back to ensure
	 * proper object validation.
	 * 
	 * @param type original Postgres internal datatype
	 * @return the converted datatype
	 */
	private String convertType(String type) {
		// Due to postgresql datatype management, we need to perform some conversion
		String convertedType = datatypeConversionMap.get(type);
		if (convertedType == null) {
			convertedType = type;
		}
		return convertedType;
	}

}
