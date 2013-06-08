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

package reactor.core.dynamic;

/**
 * A {@literal DynamicReactor} is an arbitrary interface that a proxy generator can use to wire calls to the interface
 * to appropriate {@link reactor.core.Reactor#on(reactor.fn.selector.Selector, reactor.fn.Consumer)} and {@link
 * reactor.core.Reactor#notify(Object, reactor.Event.wrap)} calls.
 *
 * @author Jon Brisbin
 */
public interface DynamicReactor {
}
