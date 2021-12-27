/*
 * This file is part of PowerTunnel.
 *
 * PowerTunnel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.plugins.sample;

import io.github.krlvm.powertunnel.sdk.http.ProxyRequest;
import io.github.krlvm.powertunnel.sdk.http.ProxyResponse;
import io.github.krlvm.powertunnel.sdk.plugin.PowerTunnelPlugin;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAdapter;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyServer;

public class SamplePlugin extends PowerTunnelPlugin {

    @Override
    public void onProxyInitialization(ProxyServer proxy) {
        this.registerProxyListener(new ProxyAdapter() {
            @Override
            public void onClientToProxyRequest(ProxyRequest request) {
                if(request.isEncrypted()) return;
                final ProxyResponse response = getServer()
                        .getProxyServer()
                        .getResponseBuilder("PowerTunnel Test Plugin")
                        .header("X-PT-Test", "OK")
                        .build();
                request.setResponse(response);
            }
        });
    }
}
