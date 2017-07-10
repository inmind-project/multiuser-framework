from __future__ import unicode_literals
from __future__ import print_function
from __future__ import division
from __future__ import absolute_import
import os

import logging


if __name__ == '__main__':
    # Running in WSGI container, configuration will be loaded from the default location
    # There is no common support for WSGI runners to pass arguments to the application, hence we need to fallback to
    # a default location for the configuration where all the settings should be placed in.
    app = create_app('frame.json')
    logging.info("Finished setting up application")
    app.run()
