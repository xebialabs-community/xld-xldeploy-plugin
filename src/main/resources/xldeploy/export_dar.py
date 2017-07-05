#
# THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
# FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
#

from com.xebialabs.deployit.plugin.api.flow import StepExitCode
from com.xebialabs.deployit.repository import RepositoryServiceHolder
from com.xebialabs.deployit.repository import WorkDir
from com.xebialabs.deployit.service.version.exporter import ExporterService
from com.xebialabs.overthere.local import LocalConnection
from com.xebialabs.overthere.local import LocalFile
from ext.deployit.plugin.xldeploy import PushToServer

from java.io import File
from java.lang import System

def get_parent_id(id):
    parent_id, name = id.rsplit("/", 1)
    return parent_id

def create_tmp_dir(local_connection):
    property = "java.io.tmpdir"
    temp_dir = System.getProperty(property)
    path_separator = local_connection.getHostOperatingSystem().getFileSeparator()
    work_dir =  "%s%swork%s" % (temp_dir,path_separator,System.currentTimeMillis())
    work_dir_as_file = File(work_dir)
    work_dir_as_file.mkdir()
    return work_dir_as_file

def remove_tmp_dir(created_tmp_dir):
    if created_tmp_dir is not None:
        if created_tmp_dir.isFile():
            created_tmp_dir.delete();
        elif created_tmp_dir.isDirectory():
            files = created_tmp_dir.listFiles()
            if files is not None:
                for file in files:
                    remove_tmp_dir(file)
            created_tmp_dir.delete()

bundle_id = deployed.getDeployable().getId()
package_id = get_parent_id(bundle_id)

# Export the package dar
context.logOutput("Exporting: %s\n" % package_id)
local_connection = LocalConnection.getLocalConnection()
repository_service = RepositoryServiceHolder.getRepositoryService()
export_service = ExporterService(repository_service)

try:
    working_directory = local_connection.getWorkingDirectory()
    if working_directory is None:
        created_tmp_dir = create_tmp_dir(local_connection)
        working_directory = LocalFile(local_connection, created_tmp_dir)
        local_connection.setWorkingDirectory(working_directory)

    work_dir = WorkDir(working_directory)
    exported_dar = export_service.exportDar(package_id, work_dir)
    context.logOutput("Completed export to file: %s" % exported_dar.getFile().getName())

    # Connect to XL Deploy instance
    use_https = deployed.getContainer().getProperty("useHttps")
    ignoreSSLWarnings = deployed.getContainer().getProperty("ignoreSSLWarnings")
    ensureSamePath = deployed.getContainer().getProperty("ensureSamePath")

    server = deployed.getContainer().getProperty("serverAddress")
    port = deployed.getContainer().getProperty("serverPort")
    username = deployed.getContainer().getProperty("username")
    password = deployed.getContainer().getProperty("password")
    protocol = "https" if use_https else "http"

    push_to_server = PushToServer()
    result = push_to_server.execute(context, get_parent_id(package_id), exported_dar, server, port, username, password, protocol, ignoreSSLWarnings, ensureSamePath, use_https)
finally:
    remove_tmp_dir(created_tmp_dir)