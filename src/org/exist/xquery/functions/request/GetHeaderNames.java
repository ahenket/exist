/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-09 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.xquery.functions.request;

import java.util.Enumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.QName;
import org.exist.http.servlets.RequestWrapper;
import org.exist.xquery.*;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.JavaObjectValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;

/**
 * @author Alain Pannetier <alain.m.pannetier@gmail.com>
 * 
 * Adjusted and Committed by Adam Retter <adam.retter@devon.gov.uk>
 */

public class GetHeaderNames extends BasicFunction {

	protected static final Logger logger = LogManager.getLogger(GetHeaderNames.class);

	public final static FunctionSignature signature =
                new FunctionSignature(
                        new QName(
                                "get-header-names",
                                RequestModule.NAMESPACE_URI,
                                RequestModule.PREFIX),
                        "Returns a sequence containing the names of all headers passed in the current request",
                        null,
                        new FunctionReturnSequenceType(Type.STRING, Cardinality.ZERO_OR_MORE, "a sequence containing the names of all headers passed in the current request"));

        /**
         * @param context
         */
        public GetHeaderNames(XQueryContext context) {
                super(context, signature);
        }

        /* (non-Javadoc)
         * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
         */
        public Sequence eval(Sequence[] args, Sequence contextSequence)
                throws XPathException {
    		
                final RequestModule myModule =
                        (RequestModule) context.getModule(RequestModule.NAMESPACE_URI);

                // request object is read from global variable $request
                final Variable var = myModule.resolveVariable(RequestModule.REQUEST_VAR);
                if(var == null || var.getValue() == null)
                        {throw new XPathException(this, ErrorCodes.XPDY0002, "No request object found in the current XQuery context.");}
                if (var.getValue().getItemType() != Type.JAVA_OBJECT)
                        {throw new XPathException(this, ErrorCodes.XPDY0002, "Variable $request is not bound to an Java object.");}
                final JavaObjectValue value = (JavaObjectValue) var.getValue().itemAt(0);
                if (value.getObject() instanceof RequestWrapper)
                {
                        final ValueSequence result = new ValueSequence();
                        for (final Enumeration<String> e = ((RequestWrapper) value.getObject()).getHeaderNames(); e.hasMoreElements();)
                        {
                                final String param = e.nextElement();
                                result.add(new StringValue(param));
                        }
                        return result;
                }
                else
                	{throw new XPathException(this, ErrorCodes.XPDY0002, "Variable $request is not bound to a Request object.");}
        }
}
