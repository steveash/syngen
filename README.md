# syngen
Synthetic record profiler and generator

This is an open-source toolkit to perform synthetic generation and 
 de-identification of human person demographics. This is a useful task when curating synthetic
 ground truth datasets for benchmarking record linkage. 

Syngen is the core library and reference implementation of the ideas discussed in Chapter 5 of the 
Ph.D. Thesis:
> Ash, Stephen M. (2017). Improving Accuracy of Patient Demographic Matching and Identity Resolution 
> (Doctoral dissertation). University of Memphis. Retrieved from https://umwa.memphis.edu/etd/

Given that you can only synthetically generate what you can model, and you can only model what
you know about, high-quality synthetic generation relies on high-quality parsing of semi-structured
text attributes. The thesis describes results using state-of-the-art parsers using modern NLP 
techniques (such as Conditional Random Fields for structured prediction). Unfortunately, 
some of those parsers cannot be released as open-source right now. Thus, syngen in this 
open-source code repository is not going to produce the same results as those described in the 
paper. However, if you have access to high-quality parsers, such as SAS Dataflux, then Syngen 
provides a sample module to illustrate how to integrate 3rd party components using a Java API
(see synthrec-sample-deps and synthrec-oss-deps modules).

A brief description of the library is described in the paper:

> To address these challenges, we propose an automated and extensible method of syn-
thetic data generation, called Syngen, which uses profiling of real data to build datasets,
which exhibit similar distributional characteristics. We use a probabilistic graphical model
to describe the real-world data-generating process as a set of interacting observable and
latent random variables. We then estimate the parameters of this model by profiling real 
datasets and smooth the model using external data sources. The smoothed model is 
transformed into the final generative model and repeatedly sampled to create new gold records
of person demographics. Starting with a realistic set of gold demographic records is the first
step in building high-quality synthetic datasets for evaluating patient matching methods.
We are releasing Syngen as an open-source toolkit with the goal that in the future 
institutions can profile, de-identify, and release synthetic datasets to researchers. A diverse
collection of publicly available, high-quality synthetic datasets will allow researchers to
quantify the merits of different matching approaches and assess the generalizability across
a set of unrelated and distinct datasets. This is the challenge that we face today when
implementing matching solutions across many facilities in different regions of the country.

