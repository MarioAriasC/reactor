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

package reactor.fn.dispatch;

import reactor.fn.Event;

/**
 * A {@link Dispatcher} implementation that executes a {@link Task} immediately in the calling thread.
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
public class SynchronousDispatcher extends BaseDispatcher {

	public static final Dispatcher INSTANCE = new SynchronousDispatcher();

	@Override
	public boolean alive() {
		return true;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void halt() {
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	protected <E extends Event<?>> Task<E> createTask() {
		return (Task<E>) new SyncTask();
	}

	private final class SyncTask extends Task<Event<?>> {
		@Override
		public void submit() {
			execute();
		}
	}
}
