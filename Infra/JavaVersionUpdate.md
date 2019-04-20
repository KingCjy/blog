아마존 리눅스 자바 7 -> 8 업데이트
============    

1. 자바 1.8 설치
<pre><code>sudo yum install -y java-1.8.0-openjdk-devel.x86_64</code></pre>

2. 자바 버전 변경
<pre><code>sudo /usr/sbin/alternatives --config java</code></pre>

3. 이전버전 삭제
<pre><code>sudo yum remove java-1.7.0-openjdk</code></pre>

4. 자바 버전 확인
<pre><code>java --version</code></pre>
