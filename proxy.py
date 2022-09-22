#!/usr/bin/env python3
from collections import namedtuple
from typing import Dict
from json import dumps, loads
from base64 import b64decode
from time import time, sleep
from threading import Thread
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from http import HTTPStatus
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler


# Set this to True if behind an ip masking proxy (with X-Forwarded-For support) while using prevent-proxy-connections
behind_proxy = False
# Should mojang accounts be allowed to join with this AltAuth proxy
allow_mojang_accounts = True
# Should banned microsoft accounts ("UserBannedException") be allowed to join?
# Should accounts with disabled multiplayer ("InsufficientPrivilegesException") be allowed to join?
allowed_microsoft_accounts = ("InsufficientPrivilegesException", "UserBannedException")


def moj_request(url, data=None):
    try:
        response = urlopen(Request(
            url,
            headers={
                "Content-Type": "application/json"
            } if data else {},
            data=dumps(data).encode("utf8") if data else None
        ))
        return response.code, response.read()
    except HTTPError as response:
        return response.code, response.read()


CachedProfile = namedtuple('CachedProfile', ('timestamp', 'use_altauth', 'ip', 'uuid'))


cached_profiles: Dict[str, CachedProfile] = {}


def timeout_cleaner():
    global cached_profiles
    while True:
        timeout = time() - 60
        cached_profiles = {
            serverId: profile
            for serverId, profile in cached_profiles.items()
            if profile.timestamp > timeout
        }
        sleep(1)


class AltAuthRequestHandler(BaseHTTPRequestHandler):

    def do_POST(self):
        try:
            self.close_connection = True

            if self.path != "/session/minecraft/join":
                self.send_response(HTTPStatus.NOT_FOUND)
                self.end_headers()
                return

            content_length = int(self.headers['Content-Length'])
            if content_length > 1024:
                raise Exception()

            request = loads(self.rfile.read(content_length))
            access_token = request["accessToken"]
            selected_profile = request["selectedProfile"]
            server_id = request["serverId"]

            token = loads(b64decode(access_token.split(".")[1] + "=="))  # Decode the JSON Web Token
            now = time()
            use_altauth = False

            if token["exp"] >= now:  # check token expiration date
                raise Exception()

            if token["iss"] == "Yggdrasil-Auth" and allow_mojang_accounts:  # Mojang account
                if token["spr"] != selected_profile:
                    raise Exception()

                # Valid token (even on other ip): 204
                # Invalid token: 403 {"error": "ForbiddenOperationException", "errorMessage": "Invalid token"}
                code, data = moj_request("https://authserver.mojang.com/validate", data={
                    "accessToken": access_token
                })
                if code != 204:
                    raise Exception()

                use_altauth = True

            else:  # Microsoft account
                # Valid token: 204
                # According to wiki.vg Xbox multiplayer disabled: InsufficientPrivilegesException
                # According to wiki.vg Multiplayer banned:        UserBannedException
                # Mojang account: 403 {"error":"ForbiddenOperationException","path":"/session/minecraft/join"}
                # Invalid token:  403 {"error":"ForbiddenOperationException","path":"/session/minecraft/join"}
                code, data = moj_request("https://sessionserver.mojang.com/session/minecraft/join", data={
                    "accessToken": access_token,
                    "selectedProfile": selected_profile,
                    "serverId": server_id
                })
                if code == 403:
                    if loads(data)["error"] in allowed_microsoft_accounts:
                        use_altauth = True
                        code = 204

            if code == 204:
                cached_profiles[server_id] = CachedProfile(
                    timestamp=now,
                    use_altauth=use_altauth,
                    ip=self.headers['X-Forwarded-For'] if behind_proxy else self.client_address[0],
                    uuid=selected_profile
                )

            self.send_response(code)
            self.end_headers()
            if code != 204 and data:
                self.wfile.write(data)
        except BaseException as e:
            print(e)

            # The client continues the login process with a 500 response, therefore 403 instead
            self.send_response(HTTPStatus.FORBIDDEN)
            self.end_headers()
            self.wfile.write(b'{"error":"ForbiddenOperationException","path":"/session/minecraft/join"}')

    def do_GET(self):
        try:
            self.close_connection = True

            if not self.path.startswith("/session/minecraft/hasJoined?"):
                self.send_response(HTTPStatus.NOT_FOUND)
                self.end_headers()
                return

            query = {
                attribute.split("=")[0]: attribute.split("=")[1]
                for attribute in self.path.split("?", 1)[1].split("&")
            }
            server_id = query["serverId"]
            username = query["username"]
            altauth_client = server_id in cached_profiles
            cached_profile = cached_profiles.pop(server_id) if altauth_client else None

            if altauth_client and "ip" in query:
                if query.pop("ip") != cached_profile.ip:
                    raise Exception()

            if altauth_client and cached_profile.use_altauth:
                code, data = moj_request(
                    f"https://sessionserver.mojang.com/session/minecraft/profile/{cached_profile.uuid}"
                )

                if data:
                    profile = loads(data)
                    if profile["name"] != username:  # Disarm server_id hash collisions and prevent username spoofing
                        raise Exception()
            elif "ip" in query:
                code, data = moj_request(
                    f"https://sessionserver.mojang.com/session/minecraft/hasJoined?username={username}&serverId={server_id}&ip={query['ip']}"
                )
            else:
                code, data = moj_request(
                    f"https://sessionserver.mojang.com/session/minecraft/hasJoined?username={username}&serverId={server_id}"
                )

            self.send_response(code)
            self.end_headers()
            self.wfile.write(data)
        except BaseException as e:
            print(e)

            self.send_response(HTTPStatus.INTERNAL_SERVER_ERROR)
            self.end_headers()


if __name__ == '__main__':
    Thread(target=timeout_cleaner, name="TimeoutCleanup", daemon=True).start()
    ThreadingHTTPServer(('127.0.0.1', 8080), AltAuthRequestHandler).serve_forever()
