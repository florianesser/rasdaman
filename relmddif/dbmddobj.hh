/*
* This file is part of rasdaman community.
*
* Rasdaman community is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Rasdaman community is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
*
* Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
rasdaman GmbH.
*
* For more information please see <http://www.rasdaman.org>
* or contact Peter Baumann via <baumann@rasdaman.com>.
*/
#ifndef _DBMDDOBJ_HH_
#define _DBMDDOBJ_HH_

#include <iostream>
class ObjectBroker;
class BaseType;
class MDDBaseType;
class OIdIf;
class r_Minterval;
class r_Error;
class DBMinterval;

#include "mddid.hh"
#include "relindexif/indexid.hh"
#include "reladminif/dbobject.hh"
#include "raslib/mddtypes.hh"
#include "relstorageif/storageid.hh"
#include "relstorageif/dbstoragelayout.hh"

//@ManMemo: Module: {\bf relmddif}
/*@Doc:
A {\tt DBMDDObj} object links all neccessary persistent data for an MDD
object.  One DBMDDObj can be inserted in multiple MDD Collections.  It will
only be deleted from the database when there are no more references in MDD
Collections.  This is done through persistent reference counting.

The DBMDDObj stores the following data:
-MDDBaseType: type information on the data that is stored in it.
-Definition domain: the spatial domain this object may extend to.  This
 domain may have open boundaries.
-Index: a refernce to the index which holds the actual data.
-Storagelayout: has asorted methods for modifying how the data is stored in the database.
The definition domain is stored in an extensible but inefficient way.
*/
/**
  * \defgroup Relmddifs Relmddif Classes
  */

/**
  * \ingroup Relmddifs
  */

class DBMDDObj  :   public DBObject
{
public:
    DBMDDObj(   const MDDBaseType* type,
                const r_Minterval& domain,
                const DBObjectId& i,
                const DBStorageLayoutId& s,
                const OId& theMDDObj) throw (r_Error);
    /*@Doc:
        type: it contains the basetype which will be used to create perstiles.
        domain: the definition domain.  the extend to which the mdd object may grow.
        i: the index structure which should be used.  there has to be some way to use DBRef<IndexDS>.
        s: the storage layout object.
        theMDDObj: this oid may not be assigned to an mdd object yet!
    */

    DBMDDObj(   const MDDBaseType* newMDDType,
                const r_Minterval& domain,
                const DBObjectId& newObjIx,
                const DBStorageLayoutId& newSL);
    /*@Doc:
        newMDDType: it contains the basetype which will be used to create perstiles.
        domain: the definition domain.  the extend to which the mdd object may grow.
        newObjIx: the index structure which should be used.  there has to be some way to use DBRef<IndexDS>.
        s: the storage layout object.
        The oid is generated by the object itself.
    */

    const MDDBaseType* getMDDBaseType() const;
    /*@Doc:
        return the mddbasetype for this object.
    */

    DBStorageLayoutId getDBStorageLayout() const;
    /*@Doc:
        return the storage layout object for this mdd object.
    */

    const char* getCellTypeName() const;
    /*@Doc:
        Returns the name of the base type of this MDD object cells.
    */

    const BaseType* getCellType() const;
    /*@Doc:
        Returns the base type of this MDD object cells.
    */

    r_Dimension dimensionality() const;
    /*@Doc:
        Returns dimensionality of the object.
    */

    r_Minterval getDefinitionDomain() const;
    /*@Doc:
        Returns the definition domain of the object.
    */

    r_Bytes getHeaderSize() const;
    /*@Doc:
        Returns the size of the header for an MDD object.
        The size returned by this funtion is an approximation to
        the size of the actual physical storage space used by the
        base DBMS. In the current implementation:
        HeaderSize = MDDBaseType*Size + r_Minterval*Size +
            DBMDDObjIxSize + DBObjectSize
        This should be reviewed and renamed to
            getPhysicalStorageSize
    */

    virtual void printStatus(unsigned int level = 0, std::ostream& stream = cout) const;
    /*@Doc:
        Prints the status of the object:
        the name of the cell type
        the definition domain
        the index contents
    */

    void setIx(const DBObjectId& newObjIx);
    /*@Doc:
        make the mdd object use newObjIx instead of its old index structure.
        the old index structure is not deleted from the database!
    */

    DBObjectId getDBIndexDS() const;
    /*@Doc:
        Returns the reference to the index.
    */

    DBMDDObj(const DBMDDObj& old);
    /*@Doc:
        Copy constructor should never be used.
        Should raise a r_Error.
    */

    virtual ~DBMDDObj();
    /*@Doc:
        Validates the object and deletes the definition domain.
    */

    virtual void setPersistent(bool t = true) throw (r_Error);
    /*@Doc:
        Was overridden to pass changes to definition domain, storage layout  and
        to the index.
    */

    virtual void setCached(bool ic);
    /*@Doc:
        overrides DBObject to handle the DBMinterval
    */

    void incrementPersRefCount();
    /*@Doc:
        used by mddset to tell the mddobj that there is an
        additional mddset ref to it
    */

    void decrementPersRefCount();
    /*@Doc:
        used by mddset to tell the mddobj that there is an mddset
        ref less to it
    */

    int getPersRefCount() const;
    /*@Doc:
        returns the number of persistent references to this object.
        when zero, the object may be deleted
    */

    virtual r_Bytes getMemorySize() const;
    /*@Doc:
        Calculates the size of this object in main memory.
    */

    DBMinterval* getNullValues() const;
    /*@Doc:
    return null values associated with this object
    */

    void setNullValues(const r_Minterval &newNullValues);
    /*@Doc:
    associate null values with this object
    */

protected:
    friend class ObjectBroker;

    DBMDDObj(const OId& id) throw (r_Error);
    /*@Doc:
        Constructs a DBMDDObj from the database.
    */

    virtual void insertInDb() throw (r_Error);
    /*@Doc:

    */

    virtual void deleteFromDb() throw (r_Error);
    /*@Doc:

    */

    virtual void readFromDb() throw (r_Error);
    /*@Doc:

    */

    virtual void updateInDb() throw (r_Error);
    /*@Doc:

    */

private:

    DBMDDObj();
    /*@Doc:
        creates an empty object and does NOT register it
        with ObjectBroker.
    */

    int persistentRefCount;
    /*@Doc:
        number of persistent references (by mddsets) to this object
    */

    const MDDBaseType* mddType;
    /*@Doc:
        Type of this object.
    */

    DBMinterval* myDomain;
    /*@Doc:
        Definition domain for the object.
    */

    DBStorageLayoutId storageLayoutId;
    /*@Doc:
        StorageLayout OId
    */

    DBObjectId objIxId;
    /*@Doc:
        Index OId
    */

    /// null values
    DBMinterval* nullValues;
};

#endif
