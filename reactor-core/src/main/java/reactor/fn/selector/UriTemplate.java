/*
 * Copyright (c) 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.fn.selector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a URI template. A URI template is a URI-like String that contains variables enclosed by braces
 * (<code>{</code>, <code>}</code>), which can be expanded to produce an actual URI.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Jon Brisbin
 * @see <a href="http://bitworking.org/projects/URI-Templates/">URI Templates</a>
 */
public class UriTemplate {

	private static final Pattern FULL_SPLAT_PATTERN     = Pattern.compile("[\\*][\\*]");
	private static final String  FULL_SPLAT_REPLACEMENT = ".*";

	private static final Pattern NAME_SPLAT_PATTERN     = Pattern.compile("\\{([^/]+?)\\}[\\*][\\*]");
	// TODO: JDK 6 doesn't support named capture groups
	//private static final String  NAME_SPLAT_REPLACEMENT = "(?<%NAME%>.*)";
	private static final String  NAME_SPLAT_REPLACEMENT = "(.*)";

	private static final Pattern NAME_PATTERN     = Pattern.compile("\\{([^/]+?)\\}");
	// TODO: JDK 6 doesn't support named capture groups
	//private static final String  NAME_REPLACEMENT = "(?<%NAME%>[^\\/.]*)";
	private static final String  NAME_REPLACEMENT = "([^\\/.]*)";

	private final List<String>                         pathVariables = new ArrayList<String>();
	private final HashMap<String, Matcher>             matchers      = new HashMap<String, Matcher>();
	private final HashMap<String, Map<String, String>> vars          = new HashMap<String, Map<String, String>>();

	private final Pattern uriPattern;

	public UriTemplate(String uriPattern) {
		String s = "^" + uriPattern;

		Matcher m = NAME_SPLAT_PATTERN.matcher(s);
		while (m.find()) {
			for (int i = 1; i <= m.groupCount(); i++) {
				String name = m.group(i);
				pathVariables.add(name);
				s = m.replaceFirst(NAME_SPLAT_REPLACEMENT.replaceAll("%NAME%", name));
				m.reset(s);
			}
		}

		m = NAME_PATTERN.matcher(s);
		while (m.find()) {
			for (int i = 1; i <= m.groupCount(); i++) {
				String name = m.group(i);
				pathVariables.add(name);
				s = m.replaceFirst(NAME_REPLACEMENT.replaceAll("%NAME%", name));
				m.reset(s);
			}
		}

		m = FULL_SPLAT_PATTERN.matcher(s);
		while (m.find()) {
			s = m.replaceAll(FULL_SPLAT_REPLACEMENT);
			m.reset(s);
		}

		this.uriPattern = Pattern.compile(s + "$");
	}

	public Pattern getUriPattern() {
		return uriPattern;
	}

	public boolean matches(String uri) {
		return matcher(uri).matches();
	}

	public Map<String, String> match(String uri) {
		Map<String, String> pathParameters = vars.get(uri);
		if (null != pathParameters) {
			return pathParameters;
		}

		pathParameters = new HashMap<String, String>();
		Matcher m = matcher(uri);
		if (m.matches()) {
			int i = 1;
			for (String name : pathVariables) {
				String val = m.group(i++);
				pathParameters.put(name, val);
			}
		}
		synchronized (vars) {
			vars.put(uri, pathParameters);
		}

		return pathParameters;
	}

	private Matcher matcher(String uri) {
		Matcher m = matchers.get(uri);
		if (null == m) {
			m = uriPattern.matcher(uri);
			synchronized (matchers) {
				matchers.put(uri, m);
			}
		}
		return m;
	}

}
