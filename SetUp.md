## Get the source: ##

svn co https://bobo-browse.googlecode.com/svn/trunk bobo-trunk

Assuming checked out to ~/bobo-trunk:

## Build: ##

### Builds jars: ###

  * cd ~/bobo-trunk
  * ant


## Run cardemo app: ##

  * In ~/bobo-trunk, do ant run-cardemo
  * cardemo should be deployed at: http://localhost:8888/cars/

cars.war is generated under cardemo/

## Build Javadocs ##

  * In ~/bobo-trunk, do ant javadoc
  * javadocs are generated under doc/