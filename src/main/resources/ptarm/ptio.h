char * const NESW = (char * const)0xFFFFFF04;
char * const gpio = (char * const)0xFFFFFF00;

void uart_outputchar(const char c) {
	char * const uart_cout = (char * const) 0xFFFF0008;
	char * const uart_dout = (char * const) 0xFFFF000C;
	while(*uart_cout != 0);
	*uart_dout = c;
}

#define printf(str) uart_outputstr(str);

void uart_outputstr(const char * str) {
	while(*str != 0){
		uart_outputchar(*str);
		str++;
	}
}


char qbuf[11];

const char * itoa(const int n) {
	int next = 0;
	int number = n;
	
	if (number < 0) {
        qbuf[next++] = '-';
		number = -number;
	}
	
	if (number == 0) {
		qbuf[next++] = '0';
	} else {
		int flag = 0;
		register int k = 100000;
		while (k > 0) {
			register int r = number / k;
			if (flag || r > 0) {
				qbuf[next++] = '0' + r;
				flag = 1;
			}
			number -= r * k;
			k = k / 10;
		}
	}
	
	qbuf[next] = 0;
	return qbuf;
}

const char * utoa(const unsigned int n) {
	int next = 0;
	unsigned int number = n;

	if (number == 0) {
		qbuf[next++] = '0';
	} else {
		int flag = 0;
		register unsigned int k = 1000000000;
		while (k > 0) {
			register unsigned int r = number / k;
			if (flag || r > 0) {
				qbuf[next++] = '0' + r;
				flag = 1;
			}
			number -= r * k;
			k = k / 10;
		}
	}
	
	qbuf[next] = 0;
	return qbuf;
}

const char * hex(const unsigned int number) {
	qbuf[0] = '0';
	qbuf[1] = 'x';
	int i, k;
	for(i = 32-4, k = 2; i >= 0; i -= 4, k++) {
		unsigned int a = (number >> i) & 0xf;
		if (a <= 9) {
			qbuf[k] = a + '0';
		} else {
			qbuf[k] = a + 'a' - 10;
		}
	}
	
	qbuf[k] = 0;
	return qbuf;
}


