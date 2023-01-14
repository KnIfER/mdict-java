/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.compress.zstd;

import io.airlift.compress.IncompatibleJvmException;

import java.nio.Buffer;

final class UnsafeUtil
{
    public static final UnsafeImpl UNSAFE;
    public static final long ADDRESS_OFFSET;

    private UnsafeUtil() {}

    static {
        UNSAFE = new UnsafeImpl();
        ADDRESS_OFFSET = UNSAFE.ADDRESS_OFFSET;
    }

    public static long getAddress(Buffer buffer)
    {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("buffer is not direct");
        }

        return UNSAFE.getLong(buffer, ADDRESS_OFFSET);
    }
}
