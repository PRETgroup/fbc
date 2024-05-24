

#define GET_MEM3(val, var, offset)			       							\
	asm volatile (	"ldr %[valbits], [%[varbits],#" #offset "] \n\t"						\
					"nop \n\t"										\
		 : [valbits]"=r"(val)								\
		 : [varbits]"r" (var)												\
		 :														\
		 )

#define GET_THREAD_ID(t_id)					\
   asm volatile (	"cdp p13, 0, cr7, cr0, cr0\n\t"	\
					"mov %[threadid], r7\n\t"	\
		 : [threadid]"=r"(t_id)							\
		 : 									\
		 : "r7"								\
		 )

#define SET_THREAD_LOCATION(t_id,label)					\
   asm volatile goto  (	"cdp p13, 0, cr7, cr0, cr0\n\t"	\
					"cmp r7, %0 \n\t"		\
					"beq %l["#label"]"		\
		 : 									\
		 : "r"(t_id)		\
		 : "r7"								\
		 : label							\
		 )


#define PT_GOTO(label)						\
   asm volatile goto ("b %l["#label"]\n\t"	\
		 : 									\
		 :									\
		 :									\
		 : label							\
		 );									\
	asm volatile ( "nop\n\t"				\
				   "nop\n\t")


#define GET_REG(val,reg)			       		\
   asm volatile ("mov %[valbits], " #reg "\n\t"	\
		 : [valbits]"=r"(val)					\
		 : 										\
		 :										\
		 )

#define PT_THREAD_STATE_INTIAL 0
#define PT_THREAD_STATE_SAVED   1
#define PT_THREAD_STATE_RUNNING 2

#define SAVE_CONTEXT_FROM_EXCEPTION(context)			\
	asm volatile ( 	"pop {lr}\n\t"						\
					"push {r0}\n\t"/*space holder for pc*/\
					"push {r0-r12}\n\t"					\
					"push {r14}\n\t");					\
	/*save exception pc*/								\
	asm volatile (	"cdp p13, 9, cr7, cr0, cr0\n\t"		\
					"str r7, [sp, #56]\n\t"/*14X4*/		\
		 :												\
		 : 												\
		 : "r7"											\
		 );												\
/*use 2 instructions so that push before ldr struct*/	\
	asm volatile (	/*save sp*/							\
					"mov %[stackbit], sp\n\t"			\
		 : [stackbit]"=r"(context.sp)					\
		 : 												\
		 :												\
	);													\
	context.state = PT_THREAD_STATE_SAVED							



#define SAVE_CONTEXT_TO_LABEL(context,resume) \
	asm volatile ( 	"push {r0}\n\t"/*space holder for pc*/\
					"push {r0-r12}\n\t"			/*push high to low, r0 on top*/		\
					"push {r14}\n\t");					\
	asm volatile ( "mov r7, %[resumeaddr] \n\t"			\
					"str r7, [sp, #56]\n\t"/*14X4*/		\
			:											\
			: [resumeaddr]"r"(resume)					\
			: "r7"										\
			);											\
/*use 2 instructions so that push before ldr struct*/	\
	asm volatile (	/*save sp*/							\
					"mov %[stackbit], sp\n\t"			\
		 : [stackbit]"=r"(context.sp)					\
		 :												\
		 :												\
	);													\
	context.state = PT_THREAD_STATE_SAVED


#define RESTORE_CONTEXT(context)							\
		context.state = PT_THREAD_STATE_RUNNING;			\
		asm volatile (	/*restore sp*/						\
						"mov sp, %[stackbit]\n\t"			\
						"pop {r14}\n\t"						\
						"pop {r0-r12}\n\t"		/*pops onto r0 to r12*/			\
						"pop {pc} \n\t"						\
					: 										\
					: 	[stackbit]"r"(context.sp)			\
					:										\
					)									

typedef struct {
	char id;
	char state;
	unsigned sp;
} PT_TCB;

#define THREAD_STACK_SIZE 0x0400

unsigned pt_max_stack = 0;

#define INITALISE_SW_THREAD_REMOTE(context, framesize, start)	\
	/*add stack*/									\
	pt_max_stack = pt_max_stack - THREAD_STACK_SIZE;	\
	context.sp = pt_max_stack;						\
	asm volatile (									\
					/*save current sp*/				\
					"mov r8, sp	\n\t"				\
					/*restore thread sp*/			\
					"mov sp, %[stack]	\n\t"		\
													\
					/*setup stack and registers*/	\
					/*update stack and fp values*/	\
					"sub	r9, sp, #4 \n\t"		\
					"sub	sp, sp, %[frame] \n\t"	\
					/*setup registers*/				\
					"mov r7, %[startaddr] \n\t"		\
					"push {r7} \n\t"	/*save pc*/	\
					"mov r7, #0 \n\t"				\
					"push {r7}	\n\t"	/*ip*/		\
					"push {r9}	\n\t"	/*fp*/		\
					"push {r7}	\n\t"	/*sl*/		\
					"push {r7}	\n\t"	/*r9*/		\
					"push {r7}	\n\t"	/*r8*/		\
					"push {r7}	\n\t"	/*r7*/		\
					"push {r7}	\n\t"	/*r6*/		\
					"push {r7}	\n\t"	/*r5*/		\
					"push {r7}	\n\t"	/*r4*/		\
					"push {r7}	\n\t"	/*r3*/		\
					"push {r7}	\n\t"	/*r2*/		\
					"push {r7}	\n\t"	/*r1*/		\
					"push {r7}	\n\t"	/*r0*/		\
					"push {r7}	\n\t"	/*r14*/		\
					/*update stack*/				\
					"mov %[stackOutput], sp \n\t"			\
					/*move sp back*/				\
					"mov sp, r8 \n\t"				\
		: [stackOutput]"=r"(context.sp)					\
		: [startaddr]"r"(start), [stack]"r" (context.sp), [frame]"r" (framesize)	\
		: "r7","r8", "r9"										\
		);											\
	context.state = PT_THREAD_STATE_RUNNING
	

#define safe_printf(str)	\
	MUTEX_LOCK();			\
	{ int i = 0; }			\
	printf(str);			\
	MUTEX_UNLOCK()

#define MUTEX_LOCK()	asm volatile("cdp p13, 1, cr0, cr7, cr0, 1\n\t")
#define MUTEX_UNLOCK()	asm volatile("cdp p13, 1, cr0, cr7, cr0, 0\n\t")
	
#define RESTORE_CONTEXT_AND_UNLOCK(context)					\
		context.state = PT_THREAD_STATE_RUNNING;			\
		asm volatile (	/*restore sp*/						\
						"mov sp, %[stackbit]\n\t"			\
						"pop {r14}\n\t"						\
						"pop {r0-r12}\n\t"		/*pops onto r0 to r12*/\
						"cdp p13, 1, cr0, cr7, cr0, 0\n\t"	/*unlock*/\
						"pop {pc} \n\t"						\
					: 										\
					: 	[stackbit]"r"(context.sp)			\
					:										\
					)	

