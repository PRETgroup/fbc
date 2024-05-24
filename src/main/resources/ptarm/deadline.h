

#define GET_TIME(high32, low32)			       					\
   asm volatile (	"cdp p13, 8, cr0, cr7, cr8\n\t"	\
					"mov %[highbit], r7\n\t"	\
					"mov %[lowbit], r8\n\t"	\
		 : [highbit]"=r"(high32), [lowbit]"=r"(low32)							\
		 : 									\
		 : "r7", "r8"								\
		 )



//return = eps - cnt
#define SUB_TIME(return_High, return_Low, epsHigh, epsLow,cntHigh, cntLow)					\
		if (epsLow < cntLow) {										\
			return_Low = (0xFFFFFFFF - cntLow) + epsLow +1;				\
			return_High = epsHigh - cntHigh -1;							\
		}else{														\
			return_Low = epsLow - cntLow;								\
			return_High = epsHigh - cntHigh;								\
		}		

#define DELAY_UNTIL_OFFSET(highns, lowns ,high32,low32) \
   asm volatile (	"mov r7, %[highbits]\n\t" \
					"mov r8, %[lowbits]\n\t"			\
					"adds r8, r8, %[lns]\n\t" \
					"adc r7, r7, %[hns]\n\t" \
					"cdp p13, 4, cr7, cr7, cr8\n\t" \
		 : \
		 : [hns]"r" (highns), [lns]"r" (lowns), [highbits]"r" (high32), [lowbits]"r" (low32)			\
		 : "r7", "r8"						\
		 )

#define DELAY_UNTIL_OFFSET_NS(nsec,high32,low32) \
   asm volatile (	"mov r7, %[highbits]\n\t" \
					"mov r8, %[lowbits]\n\t"			\
					"adds r8, r8, %[ns]\n\t" \
					"adc r7, r7, #0\n\t" \
					"cdp p13, 4, cr7, cr7, cr8\n\t" \
		 : \
		 : [ns]"r" (nsec), [highbits]"r" (high32), [lowbits]"r" (low32)			\
		 : "r7", "r8"						\
		 )

#define DELAY_FOR(nsec)						\
	{										\
		unsigned h,l;						\
		GET_TIME (h,l);						\
		DELAY_UNTIL_OFFSET_NS(nsec,h,l);	\
	}

extern unsigned int eoe_table;		// Defined by the linker script.

#define TIMING_EXCEPTION_INIT(t_id, label)							\
	{																\
		(&eoe_table)[t_id] = (unsigned)&&label;						\
	}

#define DISABLE_EXCEPTION() asm volatile ("cdp p13, 3, cr0, cr0, cr0\n\t")

#define EXCEPTION_ON_EXPIRE_OFFSET_NS(nsec,high32,low32) \
   asm volatile (	"mov r7, %[highbits]\n\t" \
					"mov r8, %[lowbits]\n\t"			\
					"adds r8, r8, %[ns]\n\t" \
					"adc r7, r7, #0\n\t" \
					"cdp p13, 2, cr7, cr7, cr8\n\t" \
		 : \
		 : [ns]"r" (nsec), [highbits]"r" (high32), [lowbits]"r" (low32)			\
		 : "r7", "r8"						\
		 )

#define EXCEPTION_ON_EXPIRE_OFFSET(nsec_h,nsec_l,high32,low32) \
   asm volatile (	"mov r7, %[highbits]\n\t" \
					"mov r8, %[lowbits]\n\t"			\
					"adds r8, r8, %[ns_l]\n\t" \
					"adc r7, r7, %[ns_h]\n\t" \
					"cdp p13, 2, cr7, cr7, cr8\n\t" \
		 : \
		 : [ns_h]"r" (nsec_h),[ns_l]"r" (nsec_l), [highbits]"r" (high32), [lowbits]"r" (low32)			\
		 : "r7", "r8"						\
		 )

	 


#define EXCEPTION_IN(nsec)							\
	{												\
		unsigned h,l;								\
		GET_TIME (h,l);								\
		EXCEPTION_ON_EXPIRE_OFFSET_NS(nsec,h,l);	\
	}




