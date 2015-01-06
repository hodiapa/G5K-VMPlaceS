
#include <string.h>
#include <stdlib.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>

#include <math.h>
#include <sys/time.h>
#include <assert.h>


#include <getopt.h>
#include <errno.h>

/* gcc -O2 -lm -std=gnu99 -Wall memtouch-with-busyloop3.c -o memtouch-with-busyloop3 */

static inline double get_duration(struct timeval *tv_sta, struct timeval *tv_end)
{
	double val = tv_end->tv_sec - tv_sta->tv_sec;
	val += 1.0L * (tv_end->tv_usec - tv_sta->tv_usec) /1000 /1000;

	return val;
}

static volatile long double do_not_optimzie = 0;
//static long double do_not_optimzie = 0;

static inline void do_a_cpu_task(void)
{
	// const long double val = 1223122312311111111111111111111111112312312312124129.192310L; // any big value is okay
	// do_not_optimzie = sqrtl(val);
	// if (errno > 0) {
	//  	fprintf(stderr, "math error\n");
	//  	exit(EXIT_FAILURE);
	// }

	{
		int i = 0;
		while (i < 1000) {
			// do_not_optimzie = 2394872312312312374L ^ 934123123723;
			do_not_optimzie = 2394872312374L ^ 933723;
			i++;
		}
	}

#if 0
	{
		char buf[100];
		sprintf(buf, "%Lf", do_not_optimzie);

		int fd = open("/dev/null", O_WRONLY);
		if (fd < 0)
			exit(EXIT_FAILURE);

		int ret = write(fd, buf, sizeof(buf));
		if (ret < 0)
			exit(EXIT_FAILURE);

		close(fd);
	}
#endif

}

static inline void do_a_mem_task(char *membuf, unsigned long i)
{
	char *ch = membuf + i * 4096;
	char tmp = *ch;
	*ch = ~tmp;
}

void gettimeofday_or_error(struct timeval *tv)
{
	int ret = gettimeofday(tv, NULL);
	if (ret < 0) {
		perror("gettimeofday");
		exit(EXIT_FAILURE);
	}
}

double get_cpu_speed(void)
{
	// const long nloops = 100000 * 10;
	const long weight = 10;
	const long nloops = 1000 * weight;

	struct timeval tv_sta;
	struct timeval tv_end;

	gettimeofday_or_error(&tv_sta);

	for (long i = 0; i < nloops; i++) {
		do_a_cpu_task();
	}

	gettimeofday_or_error(&tv_end);

	double duration = get_duration(&tv_sta, &tv_end);

	if (duration < 0.005)
		fprintf(stderr, "get_cpu_speed: warning, duration %f is too small. increase weight\n", duration);
	if (duration > 0.1)
		fprintf(stderr, "get_cpu_speed: warning, duration %f is too big. decrease weight\n", duration);



	double ret = 1.0L * nloops / duration;

	// printf("%f loops/s\n", ret);
	//
	if (ret > 100000000) {
		fprintf(stderr, "%f \n", duration);
	}

	return ret;
}

double get_mem_speed(char *membuf, unsigned long npages)
{
	const long weight = 1;
	
	struct timeval tv_sta;
	struct timeval tv_end;

	gettimeofday_or_error(&tv_sta);

	for (int repeated = 0; repeated < weight; repeated++) {
		for (unsigned long i = 0; i < npages; i++) {
			do_a_mem_task(membuf, i);
		}
	}

	gettimeofday_or_error(&tv_end);

	double duration = get_duration(&tv_sta, &tv_end);

	if (duration < 0.005)
		fprintf(stderr, "get_mem_speed: warning, duration %f is too small. increase weight\n", duration);
	if (duration > 0.1)
		fprintf(stderr, "get_mem_speed: warning, duration %f is too big. decrease weight\n", duration);

	double ret = 1.0L * npages / duration;

	// printf("duration %Lf, %f npages/s\n", duration, ret);

	return ret;
}



static inline long double do_cpu_loop(long double nloops)
{
	unsigned long count = 0;

	for (;;) {
		do_a_cpu_task();

		count += 1;
		if (count > nloops)
			break;
	}

	long double remain = nloops - (count - 1);
	return remain;
}


static unsigned long next_index = 0;
static unsigned long total_npages = 0;

static inline long double do_mem_loop(long double nloops, char *membuf, unsigned long mem_npages)
{
	unsigned long count = 0;

	for (;;) {
		do_a_mem_task(membuf, next_index);
		total_npages += 1;

		next_index += 1;
		if (next_index == mem_npages)
			next_index = 0;

		count += 1;
		if (count > nloops)
			break;
	}

	/* out target is nloops (e.g., 123.456), but we cannot do .456. */
	long double remain = nloops - (count - 1);
	return remain;
}


void *alloc_with_random(size_t memsize)
{
	const size_t pagesize = 4096;
	char *buf = malloc(pagesize);

	int fd = open("/dev/urandom", O_RDONLY);
	if (fd < 0) {
		perror("open");
		exit(EXIT_FAILURE);
	}

	int ret = read(fd, buf, pagesize);
	if (ret != pagesize) {
		perror("read");
		exit(EXIT_FAILURE);
	}

	close(fd);


	void *membuf = malloc(memsize);
	if (!membuf) {
		perror("malloc");
		exit(EXIT_FAILURE);
	}

	assert(memsize % pagesize == 0);
	unsigned long ncopy = memsize / pagesize;

	fprintf(stderr, "scrach space: memsize %zu mbytes (%zu pages)\n", memsize / 1024 / 1024, ncopy);

	for (unsigned long i = 0; i < ncopy; i++) {
		memcpy(membuf + i * pagesize, buf, pagesize);
	}

	free(buf);


	return membuf;
}

struct option longopts[] = {
	{"cmd-calibrate", no_argument, NULL, 'C'},
	{"cmd-makeload", no_argument, NULL, 'M'},
	{"cpu-speed", required_argument, NULL, 'c'},
	{"mem-speed", required_argument, NULL, 'm'},
	{NULL, 0, NULL, 0},
};

enum cmd_type {
	cmd_unknown = -1,
	cmd_calibrate,
	cmd_makeload,
} cmd = cmd_unknown;

void show_help(char *prgname)
{
	fprintf(stderr, "Memtouch (version $Id: memtouch-with-busyloop3-nobusy.c 3632 2013-07-09 15:23:35Z takahiro $)\n");
	fprintf(stderr, "%s --cmd-calibrate [size (mbytes)]\n", prgname);
	fprintf(stderr, "%s --cmd-makeload  [size (mbytes)] [speed (mbytes/s)]\n", prgname);
	fprintf(stderr, "%s --cmd-makeload --cpu-speed [..] --mem-speed [..] [size mbytes] [speed (mbytes/s)]\n", prgname);
}

void get_calibration_values(long double *loops, long double *pages, char *membuf, unsigned long npages)
{
	const int ntests = 100;
	long double loops_values[ntests];
	long double pages_values[ntests];

	for (int i = 0; i < ntests ; i++) {
		loops_values[i] = get_cpu_speed();
		pages_values[i] = get_mem_speed(membuf, npages);
	}

	long double sum = 0;

	fprintf(stderr, "loops_values: ");
	for (int i = 0; i < ntests ; i++) {
		fprintf(stderr, "%Lf ", loops_values[i]);
		sum += loops_values[i];
	}
	fprintf(stderr, "\n");

	*loops = sum / ntests;
	fprintf(stderr, "loops_values: avg %Lf\n", *loops);

	sum = 0;

	fprintf(stderr, "pages_values: ");
	for (int i = 0; i < ntests ; i++) {
		fprintf(stderr, "%Lf ", pages_values[i]);
		sum += pages_values[i];
	}
	fprintf(stderr, "\n");

	*pages = sum / ntests;
	fprintf(stderr, "pages_values: avg %Lf\n", *pages);
}

int main(int argc, char **argv)
{
	long double loops_per_second = -1;
	long double pages_per_second = -1;
	int skip_calibration = 0;

	for (;;) {
		int oindex = 0;
		int c = getopt_long_only(argc, argv, "CMc:m:", longopts, &oindex);
		if (c == -1)
			break;

		switch (c) {
			case 'C':
				cmd = cmd_calibrate;
				break;
			case 'M':
				cmd = cmd_makeload;
				break;
			case 'c':
				loops_per_second = atof(optarg);
				skip_calibration = 1;
				break;
			case 'm':
				pages_per_second = atof(optarg);
				skip_calibration = 1;
				break;

			default:
				fprintf(stderr, "command line parse error\n");
				exit(EXIT_FAILURE);
		}
	}


	if (cmd == cmd_calibrate) {
		if (argc - optind != 1) {
			/* ./a.out [size (mbytes)] */
			show_help(argv[0]);
			exit(EXIT_FAILURE);
		}
	} else if (cmd == cmd_makeload) {
		if (argc - optind != 2) {
			/* ./a.out [size (mbytes)] [speed (mbytes/s)] */
			show_help(argv[0]);
			exit(EXIT_FAILURE);
		}
	} else {
		show_help(argv[0]);
		exit(EXIT_FAILURE);
	}

	const unsigned long memsize = 1UL * atoi(argv[optind]) * 1024 * 1024;
	const unsigned long membuf_npages = memsize / 4096;
	char *membuf = alloc_with_random(memsize);





	if (cmd == cmd_calibrate) {
		/* The first pair skips page table creation. */
		get_cpu_speed();
		get_mem_speed(membuf, membuf_npages);

		get_calibration_values(&loops_per_second, &pages_per_second,  membuf, membuf_npages);

		printf("--cpu-speed %Lf --mem-speed %Lf\n", loops_per_second, pages_per_second);
		exit(EXIT_SUCCESS);
	}


	if (skip_calibration) {
		if (loops_per_second < 0 || pages_per_second < 0) {
			fprintf(stderr, "need both the speeds of cpu and mem\n");
			exit(EXIT_FAILURE);
		}
	} else {
		/* The first pair skips page table creation. */
		for (int i = 0; i < 100; i++) {
			get_cpu_speed();
			get_mem_speed(membuf, membuf_npages);
		}

		// get_cpu_speed();
		// get_mem_speed(membuf, npages);
		// loops_per_second = get_cpu_speed();
		// pages_per_second = get_mem_speed(membuf, npages);

		get_calibration_values(&loops_per_second, &pages_per_second,  membuf, membuf_npages);
	}


	fprintf(stderr, "calibrate: loops_per_second %Lf, pages_per_second %Lf\n",
			loops_per_second, pages_per_second);



	/* ./a.out [size (mbytes)] [speed (mbytes/s)] */


	unsigned long speed = 1024UL * 1024 * atoi(argv[optind + 1]);
	unsigned long speed_in_pages = speed / 4096;


	fprintf(stderr, "update speed: %lu mbytes/s (%lu pages/s)\n", speed / 1024 / 1024, speed_in_pages);
	




	/*
	 * We make the target memory update speed with our dummy CPU intensive
	 * task. The task iterates the micro busy loops and memory updates.
	 * Note that updating memory pages is also cpu intensive work.
	 *
	 * |-------------|-------------| ......(iterate)......
	 *      T_{cpu}      T_{mem}
	 *
	 * Let's consider the below situation; where
	 *   during T_{cpu}, the task performs busy loops (a times), and
	 *   during T_{mem}, the task performs memory updates (b pages).
	 *
	 * We get the capability of the given VM by measurement. Here we assume the VM can perform
	 *   busy loops by loops_per_second, and
	 *   memory upates by pages_per_second,
	 * respectively.
	 *
	 * Then, 
	 *   T_{cpu} = a / loops_per_second
	 *   T_{mem} = b / pages_per_second
	 *
	 * The target memory update speed, S (pages/s), is
	 *   S = b / (T_{cpu} + T_{mem})
	 *
	 * Therefore, we get the ratio between a and b;
	 *   a / b = loops_per_second / S - loops_per_second / pages_per_second
	 */
	


	long double a_b = loops_per_second / speed_in_pages - loops_per_second / pages_per_second;
	printf("a:b         : %Lf\n", a_b);


	/*
	 * In the below loop, we want to complete one interation in 0.1 second.
	 *
	 *   T_{cpu} + T_{mem} = 0.1
	 *   a_b * b / loops_per_second + b / pages_per_second = 0.1
	 *
	 * Then, we get b:
	 */
	const long double b = 0.01L / (a_b / loops_per_second + 1.0 / pages_per_second);


	fprintf(stderr, "cpu_loops: %Lf\n", a_b * b);
	fprintf(stderr, "mem_loops: %Lf\n", b);

	double Tcpu = a_b * b / loops_per_second;
	double Tmem = b / pages_per_second;
	fprintf(stderr, "Tcpu = %f\n", Tcpu);
	fprintf(stderr, "Tmem = %f\n", Tmem);


	unsigned long pre_total_npages = 0;
	struct timeval pre_tv;
	gettimeofday_or_error(&pre_tv);
	


	static int hoge = 0;


	long double loops_remain = 0;
	long double pages_remain = 0;

	/* touch memory */
	for (;;) {
		long double nloops = a_b * b;
		long double npages = b;

		if (loops_remain > 1) {
			nloops += 1;
			loops_remain -= 1;
		}

		if (pages_remain > 1) {
			npages += 1;
			pages_remain -= 1;
		}

		// loops_remain += do_cpu_loop(nloops);
		usleep(Tcpu * 1000 * 1000);
		pages_remain += do_mem_loop(npages, membuf, membuf_npages);


		if (hoge % 100 == 0) {
			unsigned long updated = total_npages - pre_total_npages;
			struct timeval now_tv;
			gettimeofday_or_error(&now_tv);
			double duration = get_duration(&pre_tv, &now_tv);
			printf("duration %f (sec): updated %lu (pages), speed %f (mbytes/s)\n", duration, updated, updated * 4096 / duration / 1024 / 1024);

			memcpy(&pre_tv, &now_tv, sizeof(now_tv));
			pre_total_npages = total_npages;
		}

		hoge += 1;
			
	}


	free(membuf);

	return 0;
}
