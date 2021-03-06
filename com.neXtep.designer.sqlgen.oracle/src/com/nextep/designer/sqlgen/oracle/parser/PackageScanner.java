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
package com.nextep.designer.sqlgen.oracle.parser;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import com.nextep.datadesigner.dbgm.model.ParameterType;
import com.nextep.designer.sqlgen.text.SQLWhiteSpaceDetector;

/**
 * @author Christophe Fondacci
 * @author Bruno Gautier
 */
public class PackageScanner extends RuleBasedScanner {

	public static final IToken PROC_TOKEN = new Token("__sql_procedure"); //$NON-NLS-1$
	public static final IToken PROC_ENDSPEC_TOKEN = new Token("__sql_proc_endspec"); //$NON-NLS-1$
	// public static final IToken PROC_SPEC_TOKEN= new Token("__sql_procspec");
	public static final IToken FUNC_TOKEN = new Token("__sql_function"); //$NON-NLS-1$
	public static final IToken BEGIN_TOKEN = new Token("__sql_begin"); //$NON-NLS-1$
	public static final IToken END_TOKEN = new Token("__sql_end"); //$NON-NLS-1$
	public static final IToken WORD_TOKEN = new Token("__sql_word"); //$NON-NLS-1$
	public static final IToken SEMICOLON_TOKEN = new Token("__sql_semicolon"); //$NON-NLS-1$
	public static final IToken PACKAGE_TOKEN = new Token("__sql_pkg"); //$NON-NLS-1$
	public static final IToken COMMENT_TOKEN = new Token("__sql_comment"); //$NON-NLS-1$
	public static final IToken STRING_TOKEN = new Token("__sql_string"); //$NON-NLS-1$

	public static final IToken PROC_MEMBER_TOKEN = new Token("__sql_proc_member"); //$NON-NLS-1$
	// Parameter-related tokens
	public static final IToken DECLSTART_TOKEN = new Token("__sql_declstart"); //$NON-NLS-1$
	public static final IToken DECLEND_TOKEN = new Token("__sql_declend"); //$NON-NLS-1$
	public static final IToken NEWPARAM_TOKEN = new Token("__sql_newparam"); //$NON-NLS-1$
	public static final IToken ASSIGN_TOKEN = new Token("__sql_assign"); //$NON-NLS-1$
	public static final IToken RETURN_TOKEN = new Token("__sql_return"); //$NON-NLS-1$

	public PackageScanner() {
		List<IRule> rules = new ArrayList<IRule>();
		// rules.add(new WhitespaceRule(new SQLWhiteSpaceDetector()));
		rules.add(new EndOfLineRule("--", COMMENT_TOKEN)); //$NON-NLS-1$
		rules.add(new EndOfLineRule("REM", COMMENT_TOKEN)); //$NON-NLS-1$
		rules.add(new MultiLineRule("/*", "*/", COMMENT_TOKEN)); //$NON-NLS-1$ //$NON-NLS-2$
		rules.add(new MultiLineRule("'", "'", STRING_TOKEN, '\\')); //$NON-NLS-1$ //$NON-NLS-2$

		rules.add(new CharRule('(', DECLSTART_TOKEN));
		rules.add(new CharRule(')', DECLEND_TOKEN));
		rules.add(new CharRule(',', NEWPARAM_TOKEN));
		rules.add(new CharRule('=', ASSIGN_TOKEN));
		rules.add(new CharRule(';', SEMICOLON_TOKEN));

		// rules.add(new MultiLineRule("PROCEDURE","IS",PROC_TOKEN));
		// rules.add(new MultiLineRule("PROCEDURE","AS",PROC_TOKEN));
		// rules.add(new MultiLineRule("FUNCTION","IS",PROC_TOKEN));
		// rules.add(new MultiLineRule("FUNCTION","AS",PROC_TOKEN));
		// rules.add(new MultiLineRule("CREATE","IS",PACKAGE_TOKEN));

		WordRule wr = new WordRule(new PLSQLWordDetector(), WORD_TOKEN);
		wr.addWord("BEGIN", BEGIN_TOKEN); //$NON-NLS-1$
		wr.addWord("END", END_TOKEN); //$NON-NLS-1$
		wr.addWord("PROCEDURE", PROC_TOKEN); //$NON-NLS-1$
		wr.addWord("FUNCTION", PROC_TOKEN); //$NON-NLS-1$
		wr.addWord("IS", PROC_ENDSPEC_TOKEN); //$NON-NLS-1$
		wr.addWord("AS", PROC_ENDSPEC_TOKEN); //$NON-NLS-1$
		wr.addWord("PACKAGE", PACKAGE_TOKEN); //$NON-NLS-1$
		wr.addWord("BODY", PACKAGE_TOKEN); //$NON-NLS-1$
		wr.addWord("STATIC", PROC_MEMBER_TOKEN); //$NON-NLS-1$
		wr.addWord("MEMBER", PROC_MEMBER_TOKEN); //$NON-NLS-1$
		wr.addWord("MAP", PROC_MEMBER_TOKEN); //$NON-NLS-1$
		wr.addWord("ORDER", PROC_MEMBER_TOKEN); //$NON-NLS-1$
		for (ParameterType t : ParameterType.values()) {
			wr.addWord(t.name(), new Token(t));
		}
		wr.addWord("RETURN", RETURN_TOKEN); //$NON-NLS-1$
		rules.add(wr);

		rules.add(new WhitespaceRule(new SQLWhiteSpaceDetector()));
		setRules(rules.toArray(new IRule[rules.size()]));
	}

}
