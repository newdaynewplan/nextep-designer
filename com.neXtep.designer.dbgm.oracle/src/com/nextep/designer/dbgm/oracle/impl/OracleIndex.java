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
package com.nextep.designer.dbgm.oracle.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.nextep.datadesigner.dbgm.impl.BasicIndex;
import com.nextep.datadesigner.model.ChangeEvent;
import com.nextep.datadesigner.model.IElementType;
import com.nextep.datadesigner.model.IReference;
import com.nextep.datadesigner.model.IReferenceable;
import com.nextep.designer.dbgm.model.IIndexPhysicalProperties;
import com.nextep.designer.dbgm.model.IPartition;
import com.nextep.designer.dbgm.model.IPartitionable;
import com.nextep.designer.dbgm.model.IPhysicalProperties;
import com.nextep.designer.dbgm.oracle.model.IOracleIndex;

/**
 * @author Christophe Fondacci
 * @author Bruno Gautier
 */
public class OracleIndex extends BasicIndex implements IOracleIndex {

	/** Physical properties */
	private IIndexPhysicalProperties properties;
	/** Hibernate physical properties set (workaround to handle one to one join table on a subclass) */
	private Set<IIndexPhysicalProperties> hibProps;

	public OracleIndex() {
		super();
		hibProps = new HashSet<IIndexPhysicalProperties>();
	}

	@Override
	public IPhysicalProperties getPhysicalProperties() {
		return properties;
	}

	@Override
	public void setPhysicalProperties(IPhysicalProperties properties) {
		if (properties != this.properties) {
			if (this.properties != null) {
				notifyListeners(ChangeEvent.GENERIC_CHILD_REMOVED, this.properties);
				hibProps.remove(this.properties);
			}
			this.properties = (IIndexPhysicalProperties) properties;
			if (properties != null) {
				hibProps.add((IIndexPhysicalProperties) properties);
				properties.setParent(this);
			}
			notifyListeners(ChangeEvent.GENERIC_CHILD_ADDED, this.properties);
		}
	}

	@Override
	public Map<IReference, IReferenceable> getReferenceMap() {
		Map<IReference, IReferenceable> map = new HashMap<IReference, IReferenceable>(
				super.getReferenceMap());
		if (properties != null) {
			map.put(properties.getReference(), properties);
			if (properties instanceof IPartitionable) {
				for (IPartition p : ((IPartitionable) properties).getPartitions()) {
					map.put(p.getReference(), p);
				}
			}
		}
		return map;
	}

	/**
	 * Hibernate (mother fucking) workaround
	 * 
	 * @param props
	 */
	protected void setHibernatePhysicalProperties(Set<IIndexPhysicalProperties> props) {
		if (props != null && !props.isEmpty()) {
			properties = props.iterator().next();
			properties.setParent(this);
		} else {
			properties = null;
		}
		this.hibProps = props;
	}

	/**
	 * Hibernate workaround: a Set to handle a one-one relationship
	 * 
	 * @return
	 */
	protected Set<IIndexPhysicalProperties> getHibernatePhysicalProperties() {
		return hibProps;
	}

	@Override
	public Collection<IReference> getReferenceDependencies() {
		Collection<IReference> refs = super.getReferenceDependencies();
		if (getPhysicalProperties() != null) {
			refs.addAll(getPhysicalProperties().getReferenceDependencies());
		}
		// Commented because columns already mapped through standard index columns
		// refs.addAll(getFunctionColRefMap().keySet());
		return refs;
	}

	@Override
	public boolean updateReferenceDependencies(IReference oldRef, IReference newRef) {
		if (!super.updateReferenceDependencies(oldRef, newRef)) {
			if (getPhysicalProperties() != null) {
				return getPhysicalProperties().updateReferenceDependencies(oldRef, newRef);
			}
			return false;
		}
		return true;
	}

	@Override
	public IElementType getPhysicalPropertiesType() {
		return IElementType.getInstance(IIndexPhysicalProperties.TYPE_ID);
	}
}
