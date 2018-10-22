/**
 * webソケット接続
 */

if (typeof entertainmentSocket === 'undefined') {
	var entertainmentSocket = {
		ws: null,
		create: function(vid, onMessage, reconnect) {
			var socket = this;
			
			if (this.ws == null) {
				var host = location.host;
				this.ws = new WebSocket("wss://" + host + "/socket");
			}
			
            this.ws.onopen = function(){
            	var request = {
                        "type": "connect",
                        "vid": vid
                };

            	this.send(JSON.stringify(request));
            };
            this.ws.onclose = function(){
            	if (reconnect != null) {
                	reconnect();            		
            	}
            };
            this.ws.onerror = function(event){
            	if (reconnect != null) {
                	reconnect();            		
            	}
            };
            this.ws.onmessage = function(message) {
            	onMessage(JSON.parse(message.data));
            };
		},
		send: function(message) {
			this.ws.send(message);
		}
	};
};
