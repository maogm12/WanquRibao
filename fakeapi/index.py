#!/usr/bin/env python
# encoding: utf-8

import json
import restlite
from urlparse import parse_qs
import datetime

@restlite.resource
def issues():
    def GET(request):
        with open('./data/issues.json') as issueFile:
            issues = json.load(issueFile)
        return request.response(issues, "application/json")
    return locals()

@restlite.resource
def issue():
    def GET(request):
        with open('./data/issue.json') as issueFile:
            issue = json.load(issueFile)
        return request.response(issue, "application/json")
    return locals()

@restlite.resource
def posts():
    def GET(request):
        with open('./data/post.json') as postFile:
            post = json.load(postFile)
        return request.response(post, "application/json")
    return locals()

# all the routes
routes = [
    (r'GET /issues', issues),
    (r'GET /issue', issue),
    (r'GET /posts', posts)
]

#import sae
#application = sae.create_wsgi_app(restlite.router(routes))

if __name__ == '__main__':
     # launch the server on port 8000
     from wsgiref.simple_server import make_server
     httpd = make_server('', 8034, restlite.router(routes))

     try:
         httpd.serve_forever()
     except KeyboardInterrupt:
         pass
