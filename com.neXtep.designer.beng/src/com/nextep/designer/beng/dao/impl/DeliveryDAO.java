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
package com.nextep.designer.beng.dao.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.nextep.datadesigner.hibernate.HibernateUtil;
import com.nextep.datadesigner.impl.Reference;
import com.nextep.datadesigner.model.IReference;
import com.nextep.designer.beng.dao.IDeliveryDao;
import com.nextep.designer.beng.model.IDeliveryInfo;
import com.nextep.designer.beng.model.IDeliveryModule;
import com.nextep.designer.beng.model.impl.DeliveryInfo;
import com.nextep.designer.beng.model.impl.DeliveryModule;
import com.nextep.designer.core.CorePlugin;
import com.nextep.designer.core.model.DBVendor;

/**
 * This class provides data access for Deliveries related information.
 * 
 * @author Christophe Fondacci
 * @author Bruno Gautier
 */
public class DeliveryDAO implements IDeliveryDao {

	/**
	 * This method lists summarized information for all deliveries made for the given vendor.
	 * 
	 * @param vendor vendor to look deliveries for
	 * @return a collection of {@link DeliveryInfo} objects containing deliveries summary
	 */
	@SuppressWarnings("unchecked")
	public List<IDeliveryInfo> getDeliveriesForVendor(DBVendor vendor) {
		List<IDeliveryInfo> deliveries = (List<IDeliveryInfo>) HibernateUtil.getInstance()
				.getSandBoxSession().createSQLQuery("SELECT bm.* " //$NON-NLS-1$
						+ "FROM {h-schema}beng_modules bm " //$NON-NLS-1$
						+ "  INNER JOIN {h-schema}rep_modules rm " //$NON-NLS-1$
						+ "    ON rm.version_id = bm.target_version_id " //$NON-NLS-1$
						+ "WHERE rm.dbvendor = ? ") //$NON-NLS-1$
				.addEntity("bm", DeliveryInfo.class).setString(0, vendor.name()).list(); //$NON-NLS-1$ 
		return deliveries;
	}

	@Override
	public IDeliveryModule loadModule(IDeliveryInfo info) {
		return (IDeliveryModule) CorePlugin.getIdentifiableDao().load(DeliveryModule.class,
				info.getUID(), HibernateUtil.getInstance().getSandBoxSession(), true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IDeliveryInfo> getAvailableDeliveries(IReference moduleRef) {
		return (List<IDeliveryInfo>) CorePlugin.getIdentifiableDao().loadForeignKey(
				DeliveryInfo.class, moduleRef.getUID(), "reference"); //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<IReference> lookupModuleName(String moduleName) {
		List<IReference> deliveries = (List<IReference>) HibernateUtil
				.getInstance()
				.getSandBoxSession()
				.createSQLQuery(
						"SELECT DISTINCT vr.* " //$NON-NLS-1$
								+ "FROM {h-schema}rep_modules rm " //$NON-NLS-1$
								+ "  INNER JOIN {h-schema}rep_versions rv " //$NON-NLS-1$
								+ "    ON rv.version_id = rm.version_id " //$NON-NLS-1$
								+ "  INNER JOIN {h-schema}rep_version_references vr " //$NON-NLS-1$
								+ "    ON vr.vref_id = rv.vref_id " //$NON-NLS-1$
								+ "WHERE LOWER(REPLACE(rm.module_name,' ','_')) = LOWER(REPLACE(?,' ','_')) ") //$NON-NLS-1$
				.addEntity("vr", Reference.class).setString(0, moduleName).list(); //$NON-NLS-1$
		if (deliveries != null) {
			return deliveries;
		} else {
			return Collections.emptyList();
		}
	}

}
