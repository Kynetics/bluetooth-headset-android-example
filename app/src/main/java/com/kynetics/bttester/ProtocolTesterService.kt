/*
 * Copyright © 2021-2022  Kynetics  LLC
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * @author Andrea Zoleo
 */

package com.kynetics.bttester

import retrofit2.Response
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProtocolTesterService {

    @PUT("blade/bind/{phoneaddr}")
    suspend fun bindPhone(@Path("phoneaddr") phoneAddr: String):Response<Unit>

    @PUT("blade/unbind/{phoneaddr}")
    suspend fun unbindPhone(@Path("phoneaddr") phoneAddr: String):Response<Unit>

}