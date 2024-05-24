#ifndef __ASN1__
#define __ASN1__

// NOTES:
//http://luca.ntop.org/Teaching/Appunti/asn1.html
// DER Standard:
// http://www.itu.int/ITU-T/studygroups/com17/languages/X.690-0207.pdf

// BIG TODO: Handle UDTs

///////////////////////////////////////////////////////////////////////////
// Identifier Octets: (1 Byte of identifier)
///////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////
// Class Tag:
///////////////////////////////////////////////////////////////////////////
	enum ASN1TagClass
    {
        ASN1_UNIVERSAL = 0,
        ASN1_APPLICATION = 0x40, // bit 7
        ASN1_CONTEXT = 0x80, // bit 8
        ASN1_PRIVATE = 0xC0, // bit 8 , bit 7
    };
	
	enum ASN1EncodingType 
    {
        ASN1_PRIMITIVE = 0,
        ASN1_CONSTRUCTED = 0x20, // bit 6
    };
	
	// For DER/CER BOOLEAN True requires all bits to 1
	#define ASN1_TRUE 0xFF
	
	// ISO/IEC 8824-1 (Specification of basic notation)
	// http://www.itu.int/ITU-T/studygroups/com17/languages/X.680-0207.pdf
	// 8.4 - Universal types
	
	// Also useful: Chapter III Sectio 19
	// http://www.oss.com/asn1/resources/books-whitepapers-pubs/dubuisson-asn1-book.PDF
	
	// Universal Types
	typedef enum ASN1Type
	{
		ASN1_BOOLEAN = 0x01,
		ASN1_INTEGER = 0x02,
		ASN1_BITSTRING = 0x03,
		ASN1_OCTETSTRING = 0x04,
		ASN1_NULL = 0x05,
		ASN1_OBJECTIDENTIFIER = 0x06,
		ASN1_OBJECTDESCRIPTOR = 0x07,
		ASN1_EXTERNAL = 0x08,
		ASN1_REAL = 0x09,
		ASN1_ENUM = 0x0A,
		ASN1_EMBEDDED_PDV = 0x0B,
		ASN1_UTF8STRING = 0x0C,
		ASN1_RELATIVEOBJECTID = 0x0D,
		
		ASN1_SEQUENCE = 0x10,
		ASN1_SET = 0X11,
		// Char String types:
		// 0x12 to 0x16
		ASN1_NUMERICSTRING = 0x12,
		ASN1_PRINTABLESTRING = 0x13,
		ASN1_TELETEXSTRING = 0x14,
		ASN1_VIDEOTEXSTRING = 0x15,
		ASN1_IA5STRING = 0x16,
		
		// TODO: Time  types:
		// 0x17 to 0x18
		
		// Char String types:
		// 0x19 to 0x1E
		ASN1_GRAPHICSTRING = 0x19,
		ASN1_VISIBLESTRING = 0x1A,
		ASN1_GENERALSTRING = 0x1B,
		ASN1_UNIVERSALSTRING = 0x1C,
		ASN1_CHARACTERSTRING = 0x1D,
		ASN1_BMPSTRING = 0x1E,
	}ASN1DataType;
	
// TODO: Note for implementation:
// Standard: 8.1.2.4 for Tag# >= 31


	typedef enum
    {
        //IEC_ANY = 0, // No Such thing as ANY
    IEC_BOOL = 1,
        IEC_SINT = 2,
        IEC_INT = 3,
        IEC_DINT = 4,
        IEC_LINT = 5,
        IEC_USINT = 6,
        IEC_UINT = 7,
        IEC_UDINT = 8,
        IEC_ULINT = 9,
        IEC_REAL = 10,
        IEC_LREAL = 11,
        IEC_TIME = 12,
        IEC_DATE = 13,
        IEC_TIME_OF_DAY = 14,
        IEC_DATE_AND_TIME = 15,
        IEC_STRING = 16,
        IEC_BYTE = 17,
        IEC_WORD = 18,
        IEC_DWORD = 19,
        IEC_LWORD = 20,
        IEC_WSTRING = 21,

        IEC_DerivedData = 26,
        IEC_DirectlyDerivedData = 27,
        IEC_EnumeratedData = 28,
        IEC_SubrangeData = 29,
        IEC_ARRAY = 22,
        IEC_STRUCT = 31
    }IECDataType;
	

static unsigned int IECLength[32] = {
0, // Unused
1, // IEC_BOOL
1, // IEC_SINT
2, // IEC_INT
4, // IEC_DINT
8, // IEC_LINT
1, //IEC_USINT
2, // IEC_UINT
4, // IEC_UDINT
8, // IEC_ULINT
4, // IEC_REAL
8, // IEC_LREAL
8, // IEC_TIME
8, // IEC_DATE
8, // IEC_TIME_OF_DAY
8, // IEC_DATE_AND_TIME
65535, // IEC_STRING
1, // IEC_BYTE
2, // IEC_WORD
4, // IEC_DWORD
8, // IEC_LWORD
65535, //IEC_WSTRING
0,
0,
0,
0,
0,
0,
0,
0,
0,
0
};

///////////////////////////////////////////////////////////////////////////
// :
///////////////////////////////////////////////////////////////////////////

	// IECValue used for unallocate data types
	// (Avoiding malloc)
	typedef struct {
		IECDataType dataType;
		char data[64]; // max data size = 64 :(
		unsigned int len;
	}IECValue;


int decodeDERValue(unsigned char* packet, int o, void* value, unsigned int valueSize);
int encodeDERValue(unsigned char* output, int o, IECDataType type, int arraysize, void* value);
void copyReverseBytes(unsigned char* out, int offset, unsigned char* in, unsigned int datalen);

#endif
